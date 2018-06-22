package org.openhab.binding.dsmr.internal.device.cosem3;

import java.text.ParseException;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.openhab.binding.dsmr.internal.device.cosem.CosemDouble;

public class CosemQuantityType<Q extends Quantity<Q>> extends CosemValue<QuantityType<Q>> {

    /**
     * unit of this CosemValue
     */
    private final Unit<Q> unit;

    /**
     * Creates a new {@link CosemDouble}.
     *
     * @param unit
     *            the unit of the value
     */
    public CosemQuantityType(Unit<Q> unit) {
        this.unit = unit;
    }

    /**
     * Parses a String value (that represents a double) to a Double object
     *
     * @param cosemValue
     *            the value to parse
     * @return {@link Double} on success
     * @throws ParseException
     *             if parsing failed
     */
    @Override
    protected QuantityType<Q> parse(String cosemValue) throws ParseException {
        try {
            return new QuantityType<Q>(cosemValue);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Failed to parse value " + value + " as double", 0);
        }
    }

    /**
     * Returns the unit of this Cosem value
     *
     * @return the unit of this Cosem value
     */
    // public String getUnit() {
    // return unit;
    // }

    /**
     * Returns a smart home representation of this {@link CosemDouble}
     *
     * @return {@link DecimalType} representing the value of this {@link CosemDouble}
     */
    @Override
    public QuantityType<Q> getStateValue() {
        return value;
    }
}