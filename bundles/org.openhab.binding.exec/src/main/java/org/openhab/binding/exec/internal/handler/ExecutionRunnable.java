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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.exec.internal.ExecCommandConfiguration;
import org.openhab.binding.exec.internal.handler.ExecHandler.ExecStrLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
abstract class ExecutionRunnable {

    private final Logger logger = LoggerFactory.getLogger(ExecutionRunnable.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final Runtime runtime;
    private final Function<String, String> transformFunction;
    private final ExecCommandConfiguration config;
    private final String commandLine;

    public ExecutionRunnable(Runtime rt, Function<String, String> transformFunction, ExecCommandConfiguration config) {
        this.runtime = rt;
        this.transformFunction = transformFunction;
        this.config = config;
        commandLine = config.getCommand();
    }

    public void run(@Nullable String input) {
        try {
            lock.lock();
            if (StringUtils.isNotBlank(commandLine)) {
                updateState(RUN, OnOffType.ON);

                // For some obscure reason, when using Apache Common Exec, or using a straight implementation of
                // Runtime.Exec(), on Mac OS X (Yosemite and El Capitan), there seems to be a lock race condition
                // randomly appearing (on UNIXProcess) *when* one tries to gobble up the stdout and sterr output of
                // the subprocess in separate threads. It seems to be common "wisdom" to do that in separate
                // threads, but only when keeping everything between .exec() and .waitfor() in the same thread, this
                // lockrace condition seems to go away. This approach of not reading the outputs in separate threads
                // *might* be a problem for external commands that generate a lot of output, but this will be dependent
                // on the limits of the underlying operating system.

                try {
                    ((ExecStrLookup) substitutor.getVariableResolver()).setInput(input);
                    commandLine = substitutor.replace(commandLine);
                    if (StringUtils.contains(commandLine, "${exec-input}")) {
                        logger.debug("${exec-input} is not set or the input Channel is not linked");
                        return;
                    } else if (StringUtils.contains(commandLine, "${exec-time}")) {
                        logger.debug("${exec-time} could not be transformed");
                        return;
                    }
                } catch (RuntimeException e) {
                    logger.debug("An exception occurred while formatting the command line : '{}'", e.getMessage(), e);
                    updateState(RUN, OnOffType.OFF);
                    updateState(OUTPUT, new StringType(e.getMessage()));
                    return;
                }

                logger.debug("The command to be executed is '{}'", commandLine);

                Process proc;
                try {
                    proc = runtime.exec(commandLine.toString());
                } catch (Exception e) {
                    logger.debug("An exception occurred while executing '{}' : '{}'", commandLine, e.getMessage(), e);
                    updateState(RUN, OnOffType.OFF);
                    updateState(OUTPUT, new StringType(e.getMessage()));
                    return;
                }

                final StringBuilder outputBuilder = readStream(proc.getInputStream(), "strout");
                final StringBuilder errorBuilder = readStream(proc.getErrorStream(), "stderr");

                boolean hasExited = false;
                try {
                    hasExited = proc.waitFor(config.getTimeout().intValue(), TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("An exception occurred while waiting for the process ('{}') to finish : '{}'",
                            commandLine, e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }

                if (!hasExited) {
                    logger.warn("Forcibly termininating the process ('{}') after a timeout of {} seconds", commandLine,
                            config.getTimeout().intValue());
                    proc.destroyForcibly();
                }

                updateState(RUN, OnOffType.OFF);
                updateState(EXIT, new DecimalType(proc.exitValue()));

                outputBuilder.append(errorBuilder.toString());

                final String transformedResponse = StringUtils.chomp(outputBuilder.toString());

                updateState(OUTPUT, new StringType(transformFunction.apply(transformedResponse)));
                updateState(LAST_EXECUTION, new DateTimeType());
            }
        } catch (RuntimeException e) {
            logger.warn("An exception occurred while executing the command '{}' : '{}'", config.getCommand(),
                    e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private StringBuilder readStream(InputStream inputStream, String streamType) {
        final StringBuilder outputBuilder = new StringBuilder();

        try (InputStreamReader isr = new InputStreamReader(inputStream); BufferedReader br = new BufferedReader(isr);) {
            String line = null;

            while ((line = br.readLine()) != null) {
                outputBuilder.append(line).append(System.lineSeparator());
                logger.debug("Exec [OUTPUT]: '{}'", line);
            }
        } catch (IOException e) {
            logger.debug("An exception occurred while reading the {} when executing '{}' : '{}'", streamType,
                    commandLine, e.getMessage(), e);
        }
        return outputBuilder;
    }

    abstract void updateState(String channelId, State state);
}
