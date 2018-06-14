/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.cosem2;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This CosemValueDescriptor provides meta data for a CosemValue
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored to use {@link Quantity} class as unit.
 */
@NonNullByDefault
public class CosemValueDescriptor<T extends Quantity<T>> {
    public static final String DEFAULT_CHANNEL = "default";

    /**
     * String describing the unit
     */
    private Unit<T> unit;

    /**
     * String describing the channel on which this value descriptor is available
     */
    private final String ohCannelId;

    /**
     * Creates a new CosemValueDescriptor
     *
     * @param unit the unit for the CosemValue
     * @param ohChannelId the channel for this CosemValueDescriptor
     */
    public CosemValueDescriptor(Unit<T> unit, String ohChannelId) {
        this.unit = unit;
        this.ohCannelId = ohChannelId;
    }

    /**
     * Creates a new CosemValueDescriptor with a default channel
     *
     * @param unit the unit for the CosemValue
     */
    public CosemValueDescriptor(Unit<T> unit) {
        this(unit, DEFAULT_CHANNEL);
    }

    /**
     * Returns the unit
     *
     * @return the unit
     */
    public Unit<T> getUnit() {
        return unit;
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
        return "CosemValueDescriptor[unit=" + unit + "]";
    }
}
