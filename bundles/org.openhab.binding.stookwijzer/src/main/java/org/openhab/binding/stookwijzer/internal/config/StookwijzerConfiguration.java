/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.stookwijzer.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link StookwijzerConfiguration} holds the user configuration for a Stookwijzer thing.
 *
 * The user provides RD New (EPSG:28992) coordinates for their location.
 * These are used to build the BBOX parameter for the WMS GetFeatureInfo request:
 * BBOX = x,y,x+1,y+1
 *
 * RD New coordinates for the Netherlands:
 * X range: approximately 7000 – 300000
 * Y range: approximately 289000 – 629000
 *
 * Example for Utrecht city centre: x=136372, y=457489
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class StookwijzerConfiguration {

    /**
     * X-coordinate in RD New (EPSG:28992), in metres.
     * Typical range for the Netherlands: 7000 – 300000.
     */
    public int rdX = 136372;

    /**
     * Y-coordinate in RD New (EPSG:28992), in metres.
     * Typical range for the Netherlands: 289000 – 629000.
     */
    public int rdY = 457489;

    /**
     * Polling interval in minutes. Stookwijzer updates every 6 hours,
     * so 30 minutes is a sensible default.
     */
    public int refreshInterval = 30;
}
