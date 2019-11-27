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

import org.eclipse.smarthome.core.library.types.DateTimeType;

/**
 * What is the active power consumption of a specific appliance ?
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeApplianceEvent {

    public double activePower;
    public String applianceId;
    public String timestamp;

    public String getTimestamp() {
        return getTickAsDate(timestamp);
    }

    private String getTickAsDate(String tick) {
        String date = new java.text.SimpleDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS_ISO)
                .format(new java.util.Date(Long.parseLong(tick) * 1000));
        return date;
    }
}
