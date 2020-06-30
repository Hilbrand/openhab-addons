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

import static com.pi4j.io.gpio.RaspiPin.*;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.gpio.internal.HandlerProvider;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

/**
 * Implements Raspberry PI specific code.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class RasPiHandlerProvider implements HandlerProvider<RasPiConfiguration> {

    public static DigitalPinHandler<RasPiConfiguration> newInstance(final Thing thing, final GpioController controller) {
        return new DigitalPinHandler<RasPiConfiguration>(thing, controller, new RasPiHandlerProvider());
    }

    private RasPiHandlerProvider() {
        // Create via static method.
    }

    @Override
    public GpioProvider getProvider(final RasPiConfiguration configuration)
            throws UnsupportedBusNumberException, IOException {
        return new RaspiGpioProvider(configuration.getSchema());
    }

    @Override
    public Class<RasPiConfiguration> getConfigurationClass() {
        return RasPiConfiguration.class;
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
        GPIO7(GPIO_07),
        GPIO8(GPIO_08),
        GPIO9(GPIO_09),
        GPIO10(GPIO_10),
        GPIO11(GPIO_11),
        GPIO12(GPIO_12),
        GPIO13(GPIO_13),
        GPIO14(GPIO_14),
        GPIO15(GPIO_15),
        GPIO16(GPIO_16),
        GPIO17(GPIO_17),
        GPIO18(GPIO_18),
        GPIO19(GPIO_19),
        GPIO20(GPIO_20),
        GPIO21(GPIO_21),
        GPIO22(GPIO_22),
        GPIO23(GPIO_23),
        GPIO24(GPIO_24),
        GPIO25(GPIO_25),
        GPIO26(GPIO_26),
        GPIO27(GPIO_27),
        GPIO28(GPIO_28),
        GPIO29(GPIO_29),
        GPIO30(GPIO_30),
        GPIO31(GPIO_31);

        private final Pin pin;

        private PinMapper(final Pin pin) {
            this.pin = pin;
        }
    }
}
