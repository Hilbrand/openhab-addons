/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.exec.internal.handler;

import static org.openhab.binding.exec.internal.ExecBindingConstants.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.exec.internal.ExecCommandConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ExecHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class ExecHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(ExecHandler.class);

    // List of Configurations constants
    public static final String INTERVAL = "interval";
    public static final String TIME_OUT = "timeout";
    public static final String COMMAND = "command";
    public static final String TRANSFORM = "transform";
    public static final String RUN_ON_INPUT = "runOnInput";
    public static final String REPEAT_ENABLED = "repeatEnabled";

    // RegEx to extract a parse a function String <code>'(.*?)\((.*)\)'</code>
    private static final Pattern EXTRACT_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\)");

    private final Runtime rt = Runtime.getRuntime();
    private final ItemRegistry itemRegistry;
    private final Function<String, @Nullable TransformationService> provider;
    private final WeakReference<@Nullable TransformationService> transformationServiceRef = new WeakReference<>(null);
    private @NonNullByDefault({}) ScheduledFuture<?> periodicExecutionJob;
    private @NonNullByDefault({}) ExecutionRunnable runnable;
    private @NonNullByDefault({}) ExecCommandConfiguration config;

    private String currentInput = "";
    private String previousInput = "";
    private StrSubstitutor substitutor;
    private String transformationType = "";

    public ExecHandler(Thing thing, ItemRegistry itemRegistry,
            Function<String, @Nullable TransformationService> provider) {
        super(thing);
        this.itemRegistry = itemRegistry;
        this.provider = provider;
        substitutor = new StrSubstitutor(new ExecStrLookup());
        substitutor.setEnableSubstitutionInVariables(true);
    }

    @Override
    public void initialize() {
        config = getConfigAs(ExecCommandConfiguration.class);

        runnable = new ExecutionRunnable(rt, transformFunction(config.getTransform()), config) {
            @Override
            void updateState(String channelId, State state) {
                ExecHandler.this.updateState(channelId, state);
            }
        };

        if (periodicExecutionJob == null || periodicExecutionJob.isCancelled()) {
            if (config.getInterval() != null && config.getInterval().intValue() > 0) {
                periodicExecutionJob = scheduler.scheduleWithFixedDelay(() -> runnable.run(currentInput), 0,
                        config.getInterval().intValue(), TimeUnit.SECONDS);
            }
        }

        updateStatus(ThingStatus.ONLINE);
    }

    private Function<String, String> transformFunction(String transformation) {
        if (StringUtils.isNotBlank(transformation)) {
            final String[] parts = splitTransformationConfig(transformation);
            transformationType = parts[0];

            return s -> transformString(s, parts[1]);
        } else {
            return Function.identity();
        }
    }

    @Override
    public void dispose() {
        if (periodicExecutionJob != null && !periodicExecutionJob.isCancelled()) {
            periodicExecutionJob.cancel(true);
            periodicExecutionJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ExecCommandConfiguration config = getConfigAs(ExecCommandConfiguration.class);

        if (command instanceof RefreshType || currentInput.isEmpty()) {
            return;
        }
        boolean run = false;
        if (RUN.equals(channelUID.getId())) {
            run = command == OnOffType.ON;
        } else if (INPUT.equals(channelUID.getId()) && config.getRunOnInput()) {
            currentInput = command.toString();

            if (currentInput.equals(previousInput)) {
                if (config.getRepeatEnabled()) {
                    logger.trace("Executing command '{}' because of a repetition on the input channel ('{}')",
                            config.getCommand(), command);
                    run = true;
                }
            } else {
                logger.trace("Executing command '{}' after a change of the input channel to '{}'", config.getCommand(),
                        command);
                run = true;
            }
        }
        if (run) {
            final String runInput = currentInput;

            scheduler.schedule(() -> runnable.run(runInput), 0, TimeUnit.SECONDS);
        }
    }

    private String transformString(String source, String function) {
        String transformedResponse = source;
        try {
            TransformationService transformationService = transformationServiceRef.get();

            if (transformationService == null) {
                transformationService = provider.apply(transformationType);
            }
            if (transformationService == null) {
                logger.warn("Couldn't transform response because transformationService of type '{}' is unavailable",
                        transformationType);
            } else {
                transformedResponse = transformationService.transform(function, source);
            }
        } catch (TransformationException te) {
            logger.warn("An exception occurred while transforming '{}' with '{}' : '{}'", source, config.getTransform(),
                    te.getMessage());
            // in case of an error we return the response without any transformation
        }

        logger.debug("Transformed response is '{}'", transformedResponse);
        return transformedResponse == null ? source : transformedResponse;
    }

    /**
     * Splits a transformation configuration string into its two parts - the
     * transformation type and the function/pattern to apply.
     *
     * @param transformation the string to split
     * @return a string array with exactly two entries for the type and the function
     */
    protected String[] splitTransformationConfig(String transformation) {
        Matcher matcher = EXTRACT_FUNCTION_PATTERN.matcher(transformation);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("given transformation function '" + transformation
                    + "' does not follow the expected pattern '<function>(<pattern>)'");
        }
        matcher.reset();
        matcher.find();
        final String type = matcher.group(1);
        final String pattern = matcher.group(2);

        return new String[] { type, pattern };
    }
}
