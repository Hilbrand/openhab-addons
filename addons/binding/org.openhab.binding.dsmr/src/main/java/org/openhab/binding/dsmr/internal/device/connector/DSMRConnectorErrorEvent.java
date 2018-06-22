/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.connector;

/**
 * DSMR Port error events
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Reduced number of event to only relevant errors
 */
public enum DSMRConnectorErrorEvent {
    DONT_EXISTS("Serial port does not exist"),
    IN_USE("Serial port is already in use"),
    NOT_COMPATIBLE("Serial port is not compatible"),
    UNABLE_TO_CONNECT("Could"),
    READ_ERROR("Read error");

    /**
     * Details about the event
     */
    private final String eventDetails;

    /**
     * Constructor
     *
     * @param eventDetails String containing the details about the event
     */
    DSMRConnectorErrorEvent(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    /**
     * @return the eventDetails
     */
    public String getEventDetails() {
        return eventDetails;
    }
}