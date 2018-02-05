/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.serial;

/**
 * DSMR Port events
 *
 * @author M. Volaart - Initial contribution
 */
public enum DSMRPortEvent {
    // CLOSED("Serial port closed"),
    // READ_OK("Read ok"),
    READ_ERROR("Read error"),
    // LINE_BROKEN("Serial line is broken (cable problem?)"),
    // CONFIGURATION_ERROR("Configuration error"),
    DONT_EXISTS("Serial port does not exist"),
    IN_USE("Serial port is already in use"),
    NOT_COMPATIBLE("Serial port is not compatible");
    // WRONG_BAUDRATE("Wrong baudrate");
    // ERROR("General error");

    /**
     * Details about the event
     */
    private final String eventDetails;

    /**
     * Constructor for a DSMRPortEvent
     *
     * @param eventDetails String containing the details about the event
     */
    DSMRPortEvent(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    /**
     * @return the eventDetails
     */
    public String getEventDetails() {
        return eventDetails;
    }
}