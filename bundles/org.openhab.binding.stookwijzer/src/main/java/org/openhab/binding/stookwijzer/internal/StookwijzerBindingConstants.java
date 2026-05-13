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
package org.openhab.binding.stookwijzer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link StookwijzerBindingConstants} class defines common constants for the Stookwijzer binding.
 *
 * Stookwijzer is a Dutch wood-burning advice service by RIVM that combines
 * wind speed (Beaufort scale) and air quality index (LKI) to give advice
 * per postal code area (PC4) in the Netherlands.
 *
 * Data source: https://data.rivm.nl/geo/alo/wms (WMS/WFS service)
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class StookwijzerBindingConstants {

    public static final String BINDING_ID = "stookwijzer";

    // Thing Types
    public static final ThingTypeUID THING_TYPE_STOOKWIJZER = new ThingTypeUID(BINDING_ID, "location");

    // Channel IDs — correspond to WMS feature properties
    public static final String CHANNEL_PC4 = "pc4";
    public static final String CHANNEL_MODEL_RUNTIME = "model_runtime";
    public static final String CHANNEL_LKI = "lki";
    public static final String CHANNEL_WIND = "wind";
    public static final String CHANNEL_WIND_BFT = "wind_bft";
    public static final String CHANNEL_WINDRICHTING = "windrichting";

    // Advies channels per 6-hour time block
    public static final String CHANNEL_ADVIES_0 = "advies_0";
    public static final String CHANNEL_ADVIES_6 = "advies_6";
    public static final String CHANNEL_ADVIES_12 = "advies_12";
    public static final String CHANNEL_ADVIES_18 = "advies_18";

    // Definitief channels per 6-hour time block
    public static final String CHANNEL_DEFINITIEF_0 = "definitief_0";
    public static final String CHANNEL_DEFINITIEF_6 = "definitief_6";
    public static final String CHANNEL_DEFINITIEF_12 = "definitief_12";
    public static final String CHANNEL_DEFINITIEF_18 = "definitief_18";

    // WMS endpoint
    public static final String WMS_BASE_URL = "https://data.rivm.nl/geo/alo/wms";

    // Advies values
    public static final int ADVIES_GEEN = -1;
    public static final int ADVIES_GEEL = 0;
    public static final int ADVIES_ORANJE = 1;
    public static final int ADVIES_ROOD = 2;

    // LKI range
    public static final int LKI_GEEN = -1;
    public static final int LKI_MIN = 1;
    public static final int LKI_MAX = 11;
}
