/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterType;

/**
 * The {@link DSMRBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author M. Volaart - Initial contribution
 */
public class DSMRBindingConstants {
    /**
     * Binding constant
     */
    public static final String BINDING_ID = "dsmr";

    /**
     * Binding global thread pool names
     */
    public static final String DSMR_SCHEDULED_THREAD_POOL_NAME = "DSMR Scheduled Thread Pool";

    public static final String DSMR_PORT_NAME = "org.openhab.binding.dsmr";

    /**
     * Bridge device thing
     */
    public final static ThingTypeUID THING_TYPE_DSMR_BRIDGE = new ThingTypeUID(BINDING_ID, "dsmrBridge");

    public final static Set<ThingTypeUID> ALL_THING_TYPES = new HashSet<>(DSMRMeterType.METER_THING_TYPES);

    static {
        ALL_THING_TYPES.add(THING_TYPE_DSMR_BRIDGE);
    }
}
