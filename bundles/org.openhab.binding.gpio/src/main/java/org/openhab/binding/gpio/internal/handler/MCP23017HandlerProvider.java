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
package org.openhab.binding.gpio.internal.handler;

import static com.pi4j.gpio.extension.mcp.MCP23017Pin.*;

import java.io.IOException;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.gpio.internal.HandlerProvider;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

/**
 * Implements MCP23017 specific code.
 *
 * @author Anatol Ogorek - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored mcp23017 binding into a generic gpio binding
 */
@NonNullByDefault
public class MCP23017HandlerProvider implements HandlerProvider<I2CConfiguration> {

    private @Nullable I2CDevice device;
    private int addressHex;

    public static DigitalPinHandler<I2CConfiguration> newInstance(final Thing thing, final GpioController controller) {
        return new DigitalPinHandler<I2CConfiguration>(thing, controller, new MCP23017HandlerProvider());
    }

    private MCP23017HandlerProvider() {
        // Create via static method.
    }

    @Override
    public GpioProvider getProvider(final I2CConfiguration configuration)
            throws UnsupportedBusNumberException, IOException {
        final I2CBus instance = I2CFactory.getInstance(configuration.getBusNumber());
        addressHex = configuration.getAddressHex();
        device = instance.getDevice(addressHex);
        return new MCP23017GpioProvider(configuration.getBusNumber(), configuration.getAddressHex());
    }

    @Override
    public Class<I2CConfiguration> getConfigurationClass() {
        return I2CConfiguration.class;
    }

    @Override
    public Pin getPin(final String pinName) {
        return PinMapper.valueOf(pinName.toUpperCase(Locale.ENGLISH)).pin;
    }

    @Override
    public boolean isAlive() throws IOException {
        return device != null && device.read(addressHex) >= 0;
    }

    private enum PinMapper {
        A0(GPIO_A0),
        A1(GPIO_A1),
        A2(GPIO_A2),
        A3(GPIO_A3),
        A4(GPIO_A4),
        A5(GPIO_A5),
        A6(GPIO_A6),
        A7(GPIO_A7),
        B0(GPIO_B0),
        B1(GPIO_B1),
        B2(GPIO_B2),
        B3(GPIO_B3),
        B4(GPIO_B4),
        B5(GPIO_B5),
        B6(GPIO_B6),
        B7(GPIO_B7);

        private final Pin pin;

        private PinMapper(final Pin pin) {
            this.pin = pin;
        }
    }
}
