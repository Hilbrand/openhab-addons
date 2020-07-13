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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface HandlerProvider<T> {

    /**
     *
     * @param configuration
     * @return
     * @throws UnsupportedBusNumberException
     * @throws IOException
     */
    GpioProvider getProvider(T configuration) throws UnsupportedBusNumberException, IOException;

    /**
     *
     * @return
     */
    Class<T> getConfigurationClass();

    /**
     *
     * @param channelId
     * @return
     */
    Pin getPin(String channelId);

    /**
     *
     * @return
     * @throws IOException
     */
    default boolean isAlive() throws IOException {
        return true;
    }
}
