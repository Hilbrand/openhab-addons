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
package org.openhab.binding.smappee.internal.model;

/**
 * The result of a smappee service location info reading
 * Each sensor has multiple channels, 'type' will show the use of the sensor
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeServiceLocationInfoSensorChannel {

    public String channel;
    public String name;
    public String type;
    public String ppu;
    public String uom;
    public boolean enabled;
}
