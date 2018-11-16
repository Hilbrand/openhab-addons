/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.handler;

import static org.openhab.action.pidcontroller.internal.type.PIDControllerTriggerType.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.ModuleHandlerCallback;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseTriggerModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.events.GroupItemStateChangedEvent;
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial Contribution
 */
@NonNullByDefault
public class PIDControllerTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    // public static final String UPDATE_MODULE_TYPE_ID = "pidcontroller.PIDControllerUpdateTrigger";
    // public static final String CHANGE_MODULE_TYPE_ID = "pidcontroller.PIDControllerChangeTrigger";
    public static final String MODULE_TYPE_ID = "pidcontroller.PIDControllerTrigger";

    private static final String THREAD_POOL_NAME = "pidcontrollerThreadPool";

    private final Logger logger = LoggerFactory.getLogger(PIDControllerTriggerHandler.class);
    private final PIDController controller;
    private final int loopTime;

    private @NonNullByDefault({}) ScheduledFuture<?> controllerjob;
    private ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
    private BigDecimal previousState = BigDecimal.ZERO;
    private BigDecimal state = BigDecimal.ZERO;

    @SuppressWarnings("rawtypes")
    private ServiceRegistration eventSubscriberRegistration;
    private BigDecimal input = BigDecimal.ZERO;
    private BigDecimal oldInput = BigDecimal.ZERO;

    private final String inputItemName;
    private final String setpointItemName;
    private long now;
    private long previousTime;
    private Set<String> types;

    public PIDControllerTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        Configuration cfg = module.getConfiguration();
        inputItemName = (String) cfg.get(CONFIG_INPUT_ITEM);
        setpointItemName = (String) cfg.get(CONFIG_SETPOINT_ITEM);
        // if (UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
        // this.types = Collections.singleton(ItemStateEvent.TYPE);
        // } else {
        HashSet<String> set = new HashSet<>();
        set.add(ItemStateChangedEvent.TYPE);
        set.add(GroupItemStateChangedEvent.TYPE);
        set.add(ItemStateEvent.TYPE);
        this.types = Collections.unmodifiableSet(set);
        // }
        controller = new PIDController((BigDecimal) cfg.get(CONFIG_OUTPUT_LOWER_LIMIT),
                (BigDecimal) cfg.get(CONFIG_OUTPUT_UPPER_LIMIT), (BigDecimal) cfg.get(CONFIG_KP_ADJUSTER),
                (BigDecimal) cfg.get(CONFIG_KI_ADJUSTER), (BigDecimal) cfg.get(CONFIG_KD_ADJUSTER));
        loopTime = ((BigDecimal) cfg.get(CONFIG_LOOP_TIME)).intValue();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("event.topics", "smarthome/items/" + inputItemName + "/*");
        properties.put("event.topics", "smarthome/items/" + setpointItemName + "/*");
        eventSubscriberRegistration = bundleContext.registerService(EventSubscriber.class.getName(), this, properties);
    }

    @Override
    public void setCallback(@NonNull ModuleHandlerCallback callback) {
        super.setCallback(callback);
        previousTime = System.nanoTime();
        controllerjob = scheduler.scheduleWithFixedDelay(this::calculate, 0, loopTime, TimeUnit.MILLISECONDS);
    }

    private void calculate() {
        if (controller.needsCalculation(input)) {
            previousState = state;
            now = System.nanoTime();
            state = controller.calculate(input,
                    BigDecimal.valueOf(Math.max(loopTime, TimeUnit.NANOSECONDS.toMillis(now - previousTime))));
            previousTime = now;
            if (!previousState.equals(state)) {
                Map<String, Object> outputs = new HashMap<String, Object>();
                outputs.put(OUTPUT, state);
                ((TriggerHandlerCallback) callback).triggered(module, outputs);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        eventSubscriberRegistration.unregister();
        if (controllerjob != null) {
            controllerjob.cancel(true);
            controllerjob = null;
        }
    }

    @Override
    public boolean apply(@Nullable Event event) {
        logger.trace("->FILTER: {}:{} and setpoint: {}", event.getTopic(), inputItemName, setpointItemName);
        return event.getTopic().contains("smarthome/items/" + inputItemName + "/")
                || event.getTopic().contains("smarthome/items/" + setpointItemName + "/");
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return this;
    }

    @Override
    public void receive(Event event) {
        // BigDecimal input = (BigDecimal) context.get(INPUT);
        // BigDecimal setpoint = (BigDecimal) module.getConfiguration().get(PIDControllerConditionType.CONFIG_SETPOINT);

        if (callback != null) {
            // logger.trace("Received Event: Source: {} Topic: {} Type: {} Payload: {}", event.getSource(),
            // event.getTopic(), event.getType(), event.getPayload());
            // if (event instanceof ItemStateEvent && UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            // input = (BigDecimal) ((ItemStateEvent) event).getItemState();
            // } else
            if (event instanceof ItemStateEvent && MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ItemStateEvent ie = (ItemStateEvent) event;
                extracted(ie.getItemName(), ((DecimalType) ie.getItemState()).toBigDecimal(), null);
            } else if (event instanceof ItemStateChangedEvent && MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ItemStateChangedEvent ie = (ItemStateChangedEvent) event;
                extracted(ie.getItemName(), ((DecimalType) ie.getItemState()).toBigDecimal(),
                        ((DecimalType) ie.getOldItemState()).toBigDecimal());
            }
            // State state = ((ItemStateChangedEvent) event).getItemState();
            //
            // if (stateMatches(this.state, state) && stateMatches(this.previousState, oldState)) {
            // values.put("oldState", oldState);
            // values.put("newState", state);
            // }
        }
    }

    private void extracted(String itemName, BigDecimal newState, @Nullable BigDecimal oldState) {
        if (CONFIG_INPUT_ITEM.equals(itemName)) {
            oldInput = oldState == null ? input : oldState;
            input = newState;
        } else if (CONFIG_SETPOINT_ITEM.equals(itemName)) {
            controller.setSetPoint(newState);
        }
    }

    private boolean stateMatches(BigDecimal state, State eventState) {
        return eventState instanceof DecimalType && state.equals(((DecimalType) eventState).toBigDecimal());
    }
}
