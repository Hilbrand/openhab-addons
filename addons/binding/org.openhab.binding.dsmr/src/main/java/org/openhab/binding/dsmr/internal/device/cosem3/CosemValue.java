/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.cosem3;

import java.text.ParseException;

import org.eclipse.smarthome.core.types.State;

/**
 * CosemValue represents the mapping between COSEM formatted values and {@link State} type values
 *
 * @author M. Volaart - Initial contribution
 * @param <S> the native type this CosemValue represent
 */
public abstract class CosemValue<S extends Object> {
    /** native value */
    protected S value;

    /**
     * Parses the string value to the {@link State} value
     *
     * @param CosemValue the Cosem value to parse
     * @return S the native object type of this Cosem value
     * @throws ParseException if parsing failed
     */
    protected abstract S parse(String cosemValue) throws ParseException;

    /**
     * Sets the value of this CosemValue
     *
     * This method will automatically parse the unit and the value of the COSEM
     * value string
     *
     * @param cosemValue the cosemValue
     * @throws ParseException if parsing failed
     */
    public void setValue(String cosemValue) throws ParseException {
        value = parse(cosemValue);
    }

    /**
     * Return the Cosem value
     *
     * @return native object value
     */
    public S getValue() {
        return value;
    }

    /**
     * Returns the {@link State} object representing the Cosem value
     *
     * @return {@link State} object representing the Cosem value
     */
    public abstract State getStateValue();

    /**
     * Returns String representation of the Cosem value
     *
     * @return String representation of the Cosem value
     */
    @Override
    public String toString() {
        if (value == null) {
            return "CosemValue is not initialized yet";
        } else {
            return value.toString();
        }
    }
}
