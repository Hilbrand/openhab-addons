/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.cosem3;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.Nullable;

/**
 * This CosemValueDescriptor provides meta data for a CosemValue
 *
 * @author M. Volaart - Initial contribution
 * @param <Q>
 */
// @NonNullByDefault
public class CosemValueDescriptor<Q extends Quantity<Q>> {
    public static final String DEFAULT_CHANNEL = "default";

    /**
     * Class describing the type
     */
    private final Class<? extends CosemValue<? extends Object>> cosemValueClass;

    /**
     * String describing the unit
     */
    // private final String unit;

    /* String describing the channel on which this value descriptor is available */
    private final String ohCannelId;

    @Nullable
    private Unit<Q> unit2 = null;

    /**
     * Creates a new CosemValueDescriptor
     *
     * @param cosemValueClass the CosemValue class that the CosemValueDescriptor represent
     * @param unit the unit for the CosemValue
     * @param ohChannelId the channel for this CosemValueDescriptor
     */
    public CosemValueDescriptor(Class<? extends CosemValue<? extends Object>> cosemValueClass, String ohChannelId) {
        this.cosemValueClass = cosemValueClass;
        this.unit2 = null;
        this.ohCannelId = ohChannelId;
    }

    /**
     * Creates a new CosemValueDescriptor
     *
     * @param cosemValueClass the CosemValue class that the CosemValueDescriptor represent
     * @param unit the unit for the CosemValue
     * @param ohChannelId the channel for this CosemValueDescriptor
     */
    public CosemValueDescriptor(Class<? extends CosemValue<? extends Object>> cosemValueClass, Unit<Q> unit,
            String ohChannelId) {
        this.cosemValueClass = cosemValueClass;
        this.unit2 = unit;
        this.ohCannelId = ohChannelId;
    }

    /**
     * Creates a new CosemValueDescriptor with a default channel
     *
     * @param cosemValueClass the CosemValue class that the CosemValueDescriptor represent
     * @param unit the unit for the CosemValue
     */
    public CosemValueDescriptor(Class<? extends CosemValue<? extends Object>> cosemValueClass) {
        this(cosemValueClass, null, DEFAULT_CHANNEL);
    }

    public CosemValueDescriptor(Unit<Q> unit) {
        unit2 = unit;
        this.ohCannelId = "";
        this.cosemValueClass = (Class<? extends CosemValue<? extends Object>>) CosemQuantityType.class;
    }

    /**
     * Returns the class of the CosemValue
     *
     * @return the class of the CosemValue
     */
    public Class<? extends CosemValue<? extends Object>> getCosemValueClass() {
        return cosemValueClass;
    }

    /**
     * Returns the unit
     *
     * @return the unit
     */
    public @Nullable Unit<Q> getUnit() {
        return unit2;
    }

    /**
     * Returns the channel id for this CosemValueDescriptor
     *
     * @return the channel identifier
     */
    public String getChannelId() {
        return ohCannelId;
    }

    /**
     * Returns String representation of this CosemValueDescriptor
     *
     * @return String representation of this CosemValueDescriptor
     */
    @Override
    public String toString() {
        return "CosemValueDescriptor[class=" + cosemValueClass.toString() + ", unit=" + unit2 + "]";
    }
}
