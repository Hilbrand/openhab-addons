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

import com.pi4j.io.gpio.RaspiPinNumberingScheme;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class RasPiConfiguration extends GpioConfiguration {

    private static final String BCM = "BCM";
    private static final String WPI = "WPI";

    private String pinSchema = BCM;

    public RaspiPinNumberingScheme getSchema() {
        switch (pinSchema) {
            case WPI:
                return RaspiPinNumberingScheme.DEFAULT_PIN_NUMBERING;
            case BCM:
            default:
                return RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING;
        }
    }
}
