/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.cosem2;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.smarthome.core.library.types.QuantityType;
import org.openhab.binding.dsmr.internal.device.cosem.CosemValue;
import org.openhab.binding.dsmr.internal.device.cosem.OBISIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for Cosem Object implementation
 *
 * @author M. Volaart - Initial contribution
 */
public class CosemObject {

    /**
     * Regular expression for finding CosemValues
     */
    private static final Pattern COSEM_VALUES_PATTERN = Pattern.compile("(\\(([^\\(\\)]*)\\))");
    /**
     * Check if COSEM value has a unit, check and parse the value. We assume here numbers (float or integers)
     * The specification states that the delimiter between the value and the unit is a '*'-character.
     * We have seen on the Kaifa 0025 meter that both '*' and the '_' character are used.
     *
     * On the Kampstrup 162JxC in some CosemValues the separator is missing
     *
     * The above quirks are supported
     *
     * We also support unit that do not follow the exact case.
     */
    private static final Pattern COSEM_VALUE_WITH_UNIT_PATTERN = Pattern.compile("^(\\d+\\.?\\d+)[\\*_]?(.+)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COSEM_VALUE_WITH_UNIT_PATTERN_2 = Pattern.compile("^(\\d+\\.?\\d+[\\*_]?.+)$",
            Pattern.CASE_INSENSITIVE);

    private final Logger logger = LoggerFactory.getLogger(CosemObject.class);

    /**
     * CosemObject type
     */
    private final CosemObjectType type;

    /**
     * The actual OBISIdentifier for this CosemObject
     */
    private final OBISIdentifier obisIdentifier;

    /**
     * List of COSEM value in this message
     */
    private Map<String, QuantityType> cosemValues;

    /**
     * Construct a new CosemObject with the specified OBIS Message Type
     *
     * @param msgType
     *            {@link CosemObjectType}
     */
    public CosemObject(CosemObjectType msgType, OBISIdentifier obisIdentifier) {
        this.type = msgType;
        this.obisIdentifier = obisIdentifier;

        cosemValues = new HashMap<>();
    }

    /**
     * Return the {@link CosemObjectType} for this Cosem Object
     *
     * @return the {@link CosemObjectType} for this Cosem Object
     */
    public CosemObjectType getType() {
        return type;
    }

    /**
     * @return the obisIdentifier
     */
    public OBISIdentifier getObisIdentifier() {
        return obisIdentifier;
    }

    /**
     * Returns string representation of this Cosem Object
     *
     * @return string representation of this Cosem Object
     */
    @Override
    public String toString() {
        return "Cosem Object(type:" + type + ", cosemValues:" + cosemValues + ")";
    }

    /**
     * Returns the Cosem values that are part of this Cosem Object
     *
     * @return List of {@link CosemValue} that are part of this Cosem Object
     */
    public Map<String, QuantityType> getCosemValues() {
        return cosemValues;
    }

    /**
     * Parses the List of COSEM String value to COSEM objects values.
     * <p>
     * When the parser has problems it throws an {@link ParseException}. The
     * already parsed values will still be available. It is up to the caller how
     * to handle a partially parsed message.
     *
     * @param cosemStringValues
     *            the List of COSEM String values
     * @throws ParseException
     *             if parsing fails
     */
    public void parseCosemValues(String cosemValueString) throws ParseException {
        logger.trace("Parsing CosemValue string {}", cosemValueString);

        Matcher cosemValueMatcher = COSEM_VALUES_PATTERN.matcher(cosemValueString);

        int nrOfCosemValues = 0;
        while (cosemValueMatcher.find()) {
            nrOfCosemValues++;
        }
        // We need the matcher again, reset to initial state
        cosemValueMatcher.reset();

        if (type.supportsNrOfValues(nrOfCosemValues)) {
            logger.trace("Received items: {} is supported", nrOfCosemValues);

            int cosemValueItr = 0;
            while (cosemValueMatcher.find()) {
                String cosemStringValue = cosemValueMatcher.group(2);
                CosemValueDescriptor valueDescriptor = type.getDescriptor(cosemValueItr);
                QuantityType cosemValue = parseCosemValue2(valueDescriptor.getUnit(), cosemStringValue);

                if (cosemValue != null) {
                    if (!cosemValues.containsKey(valueDescriptor.getChannelId())) {
                        cosemValues.put(valueDescriptor.getChannelId(), cosemValue);
                    } else {
                        logger.warn("Value for descriptor {} already exists, dropping value {}", valueDescriptor,
                                cosemValue);
                    }
                }
                cosemValueItr++;
            }
        } else {
            throw new ParseException(type + " does not support " + nrOfCosemValues + " items", 0);
        }
    }

    /**
     * Creates an empty CosemValue object
     *
     * @param cosemValueDescriptor
     *            the CosemValueDescriptor object that describes the CosemValue
     * @return the instantiated CosemValue based on the specified
     *         CosemValueDescriptor
     * @throws ParseException if a CosemValue could not be created
     */
    private <T extends Quantity<T>> QuantityType<T> parseCosemValue2(Unit<T> unit2, String cosemValueString)
            throws ParseException {

        QuantityType<T> q = new QuantityType<T>(cosemValueString);
        if (unit2.equals(q.getUnit())) {
            return q;
        } else {
            logger.trace("Unit of {} is not same as expected unit:{}", cosemValueString, unit2);
            return null;
        }
    }
}
