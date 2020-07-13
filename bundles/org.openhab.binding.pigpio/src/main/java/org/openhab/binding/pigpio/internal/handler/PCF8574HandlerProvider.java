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

import static com.pi4j.gpio.extension.pcf.PCF8574Pin.*;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.pigpio.internal.HandlerProvider;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

/**
 * Implements PCF8574 specific code.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class PCF8574HandlerProvider implements HandlerProvider<I2CConfiguration> {

    public static DigitalPinHandler<I2CConfiguration> newInstance(final Thing thing, final GpioController controller) {
        return new DigitalPinHandler<I2CConfiguration>(thing, controller, new PCF8574HandlerProvider());
    }

    private PCF8574HandlerProvider() {
        // Create via static method.
    }

    @Override
    public GpioProvider getProvider(final I2CConfiguration configuration)
            throws UnsupportedBusNumberException, IOException {
        return new PCF8574GpioProvider(configuration.getBusNumber(), configuration.getAddressHex());
    }

    @Override
    public Class<I2CConfiguration> getConfigurationClass() {
        return I2CConfiguration.class;
    }

    @Override
    public Pin getPin(final String pinName) {
        return PinMapper.valueOf("GPIO" + pinName).pin;
    }

    private enum PinMapper {
        GPIO0(GPIO_00),
        GPIO1(GPIO_01),
        GPIO2(GPIO_02),
        GPIO3(GPIO_03),
        GPIO4(GPIO_04),
        GPIO5(GPIO_05),
        GPIO6(GPIO_06),
        GPIO7(GPIO_07);

        private final Pin pin;

        private PinMapper(final Pin pin) {
            this.pin = pin;
        }
    }
}
