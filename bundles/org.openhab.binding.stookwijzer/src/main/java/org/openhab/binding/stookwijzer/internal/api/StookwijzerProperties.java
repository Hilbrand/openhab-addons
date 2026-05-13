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
package org.openhab.binding.stookwijzer.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link StookwijzerProperties} is a DTO that maps the JSON properties returned
 * by the RIVM WMS GetFeatureInfo endpoint for the stookwijzer_v2 layer.
 *
 * Example response:
 *
 * <pre>
 * {
 *   "pc4": "3515",
 *   "model_runtime": "13-05-2026 10:00",
 *   "lki": 1,
 *   "wind": 4,
 *   "wind_bft": 3,
 *   "windrichting": -1,
 *   "advies_0": 0,
 *   "advies_6": 0,
 *   "advies_12": 2,
 *   "advies_18": 2,
 *   "definitief_0": true,
 *   "definitief_6": true,
 *   "definitief_12": false,
 *   "definitief_18": false
 * }
 * </pre>
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class StookwijzerProperties {

    /** 4-digit postal code area for which the advice is calculated. */
    public @Nullable String pc4;

    /** Date and time when the model calculation was run. Format: "dd-mm-yyyy hh:mm". */
    public @Nullable String model_runtime;

    /**
     * Calculated Air Quality Index (LuchtKwaliteitsIndex) for the first time block.
     * Domain: [-1, 1..11] where -1 = no value, 1 = good, 11 = very poor.
     */
    public int lki = -1;

    /**
     * Average wind speed in the first time block (0–6 h after model_runtime), in m/s.
     */
    public double wind = 0.0;

    /**
     * Average wind speed in the first time block (0–6 h after model_runtime), in Beaufort.
     */
    public int wind_bft = 0;

    /**
     * Wind direction in degrees (0–360), or -1 if unavailable.
     */
    public int windrichting = -1;

    /**
     * Advice for time block 0–6 h after model_runtime.
     * Domain: [-1=geen, 0=geel, 1=oranje, 2=rood]
     */
    public int advies_0 = -1;

    /**
     * Advice for time block 6–12 h after model_runtime.
     * Domain: [-1=geen, 0=geel, 1=oranje, 2=rood]
     */
    public int advies_6 = -1;

    /**
     * Advice for time block 12–18 h after model_runtime.
     * Domain: [-1=geen, 0=geel, 1=oranje, 2=rood]
     */
    public int advies_12 = -1;

    /**
     * Advice for time block 18–24 h after model_runtime.
     * Domain: [-1=geen, 0=geel, 1=oranje, 2=rood]
     */
    public int advies_18 = -1;

    /** Whether the advice for time block 0–6 h is final (no longer subject to change). */
    public boolean definitief_0 = false;

    /** Whether the advice for time block 6–12 h is final. */
    public boolean definitief_6 = false;

    /** Whether the advice for time block 12–18 h is final. */
    public boolean definitief_12 = false;

    /** Whether the advice for time block 18–24 h is final. */
    public boolean definitief_18 = false;
}
