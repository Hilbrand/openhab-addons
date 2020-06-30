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
package org.openhab.binding.gpio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link GPIOBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class GPIOBindingConstants {

    private static final String BINDING_ID = "gpio";
    private static final String MCP23017_ID = "mcp23017";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_MCP23017 = new ThingTypeUID(BINDING_ID, MCP23017_ID);

    public static final String DEFAULT_STATE = "default_state";
    public static final String I2C_ADDRESS = "address";
    public static final String I2C_BUS_NUMBER = "bus_number";
    public static final String PIN = "pin";

    public static final String CHANNEL_ACTIVE_LOW = "active_low";
    public static final String CHANNEL_PULL_MODE = "pull_mode";
    public static final String CHANNEL_PULL_MODE_DEFAULT = "OFF";

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

    public static final String CHANNEL_A0 = "A0";
    public static final String CHANNEL_A1 = "A1";
    public static final String CHANNEL_A2 = "A2";
    public static final String CHANNEL_A3 = "A3";
    public static final String CHANNEL_A4 = "A4";
    public static final String CHANNEL_A5 = "A5";
    public static final String CHANNEL_A6 = "A6";
    public static final String CHANNEL_A7 = "A7";
    public static final String CHANNEL_B0 = "B0";
    public static final String CHANNEL_B1 = "B1";
    public static final String CHANNEL_B2 = "B2";
    public static final String CHANNEL_B3 = "B3";
    public static final String CHANNEL_B4 = "B4";
    public static final String CHANNEL_B5 = "B5";
    public static final String CHANNEL_B6 = "B6";
    public static final String CHANNEL_B7 = "B7";

}
