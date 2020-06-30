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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class I2CConfiguration extends GpioConfiguration {

    private final String address = "20";
    private final int busNumber = 1;
    private final int pollingInterval = 50;

    public String getAddress() {
        return address;
    }

    public int getAddressHex() {
        return Integer.parseInt(address, 16);
    }

    public int getBusNumber() {
        return busNumber;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }
}
