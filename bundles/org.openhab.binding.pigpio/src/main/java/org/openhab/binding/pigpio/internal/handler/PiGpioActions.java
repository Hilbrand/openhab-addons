/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.pigpio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.core.thing.binding.ThingActionsScope;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.pigpio.internal.GPIOBindingConstants;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@ThingActionsScope(name = GPIOBindingConstants.BINDING_ID)
@NonNullByDefault
public class PiGpioActions implements ThingHandlerService, ThingActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(PiGpioActions.class);

    private volatile @Nullable static PiGpioActions actions;
    private @Nullable DigitalPinHandler<?> handler;

    @Override
    public void setThingHandler(@Nullable final ThingHandler handler) {
        if (handler == null) {
            this.handler = null;
            return;
        }
        actions = this;
        LOGGER.info("SetTHingHandler: static:{}", actions);
        if (handler instanceof DigitalPinHandler) {
            this.handler = (DigitalPinHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }

    @RuleAction(label = "blink", description = "Give a blink signal on an output pin.")
    public void blink(
            @ActionInput(name = "channel", description = "The channel to blink") final String channel,
            @ActionInput(name = "delay", description = "") final long delay,
            @ActionInput(name = "duration", description = "Duration of the blink action in milliseconds") final long duration) {
        LOGGER.debug("Action blink({}, {}, {}) called", channel, delay, duration);
        final DigitalPinHandler<?> localHandler = handler;

        if (localHandler == null) {
            LOGGER.debug("PiGpio: Action service ThingHandler is null.");
        } else {
            localHandler.blink(channel, delay, duration);
        }
    }

    public static void blink(@Nullable final ThingActions actions, final String channel, final long delay, final long duration) {
        LOGGER.info("OLD blink({}, {}, {}) called", channel, delay, duration);
        final PiGpioActions globalActions = PiGpioActions.actions;

        LOGGER.info("Blink: static:{}", globalActions);
        if (actions != null && PiGpioActions.class.getName().equals(actions.getClass().getName()) && globalActions != null) {
            LOGGER.info("BLINKING!");
            globalActions.blink(channel, delay, duration);
        } else {
            LOGGER.debug("Actions called doesn't match this actions class:{}", actions);
        }
    }
}
