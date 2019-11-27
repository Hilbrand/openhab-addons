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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The result of a smappee reading, the energy consumption
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeDeviceReadingConsumption {

    public long timestamp;
    public double consumption;
    public double solar;
    public double alwaysOn;

    public LocalDateTime timestampDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
    }
}
