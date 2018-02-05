/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.cosem;

import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Map<String, CosemValue<? extends Object>> cosemValues;

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
    public Map<String, ? extends CosemValue<? extends Object>> getCosemValues() {
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
                CosemValue<? extends Object> cosemValue = parseCosemValue(valueDescriptor, cosemStringValue);

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
    private CosemValue<? extends Object> parseCosemValue(CosemValueDescriptor cosemValueDescriptor,
            String cosemValueString) throws ParseException {
        Class<? extends CosemValue<? extends Object>> cosemValueClass = cosemValueDescriptor.getCosemValueClass();

        String unit = cosemValueDescriptor.getUnit();
        String value = matchValue(cosemValueDescriptor, cosemValueString);

        if (value == null) {
            logger.trace("Failed to parse: {} for OBISMsgType: {}", cosemValueString, type);
            return null;
        } else {
            try {
                Constructor<? extends CosemValue<? extends Object>> c = cosemValueClass.getConstructor(String.class);

                CosemValue<? extends Object> cosemValue = c.newInstance(unit);
                cosemValue.setValue(value);

                return cosemValue;
            } catch (ReflectiveOperationException roe) {
                logger.warn("Failed to create {} message", type.obisId, roe);
                return null;
            }
        }
    }

    /**
     *
     * @param cosemValueDescriptor
     * @param cosemValueString
     * @return
     * @throws ParseException
     */
    private String matchValue(CosemValueDescriptor cosemValueDescriptor, String cosemValueString)
            throws ParseException {
        String unit = cosemValueDescriptor.getUnit();
        String value;

        if (unit.isEmpty()) {
            value = cosemValueString;
        } else {
            Matcher m = COSEM_VALUE_WITH_UNIT_PATTERN.matcher(cosemValueString);

            if (m.matches()) {
                if (unit.equalsIgnoreCase(m.group(2))) {
                    value = m.group(1);
                } else {
                    logger.trace("Unit of {} is not same as expected unit:{}", cosemValueString, unit);
                    value = null;
                }
            } else {
                throw new ParseException("Unit of " + cosemValueString + " is not " + unit, 0);
            }
        }
        return value;
    }
}
