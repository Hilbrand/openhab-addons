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
package org.openhab.binding.pigpio.internal;

import static org.openhab.binding.pigpio.internal.GPIOBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.pigpio.internal.handler.MCP23008HandlerProvider;
import org.openhab.binding.pigpio.internal.handler.MCP23017HandlerProvider;
import org.openhab.binding.pigpio.internal.handler.PCF8574HandlerProvider;
import org.openhab.binding.pigpio.internal.handler.RasPiHandlerProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.impl.GpioControllerImpl;
import com.pi4j.wiringpi.GpioUtil;

/**
 * The {@link GPIOHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.gpio", service = ThingHandlerFactory.class)
public class GPIOHandlerFactory extends BaseThingHandlerFactory {

    public final GpioController controller;

    @Activate
    public GPIOHandlerFactory() {
        GpioUtil.enableNonPrivilegedAccess();
        controller = new GpioControllerImpl();
    }

    @Deactivate
    public void deactivate() {
        controller.shutdown();
    }

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        final ThingHandler handler;

        switch (thingTypeUID.getId()) {
            case RASPBERRY_PI_ID:
                handler = RasPiHandlerProvider.newInstance(thing, controller);
                break;
            case MCP23008_ID:
                handler = MCP23008HandlerProvider.newInstance(thing, controller);
                break;
            case MCP23017_ID:
                handler = MCP23017HandlerProvider.newInstance(thing, controller);
                break;
            case PCF8574_ID:
                handler = PCF8574HandlerProvider.newInstance(thing, controller);
                break;
            default:
                handler = null;
        }
        return handler;
    }
}
