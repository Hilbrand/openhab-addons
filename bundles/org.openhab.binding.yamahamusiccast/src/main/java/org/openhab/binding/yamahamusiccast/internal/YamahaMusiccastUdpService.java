/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.yamahamusiccast.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.yamahamusiccast.internal.dto.UdpMessage;

/**
 * Interface for a service to communicate UDP messages.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface YamahaMusiccastUdpService {

    /**
     * Handler to process UDP events.
     * The service will first check if the UDP message to process should be process by the handler by calling
     * {@link #isDeviceId(String)}.
     * If that returns true if will pass the UDP message to the handler in {@link #processUDPEvent(UdpMessage, String)}.
     */
    interface YamahaMusiccastUdpMessageHandler {
        /**
         * Processes the given UdpMessage with a unique trackingID.
         *
         * @param udpMessage UDP message
         * @param trackingID unique tracking id
         */
        void processUDPEvent(UdpMessage udpMessage, String trackingID);

        /**
         * Return true if the message should be processed by the handler given the udp device id.
         *
         * @param udpDeviceId UDP device id to check if it should process a message
         * @return true if handler should process message
         */
        boolean isDeviceId(String udpDeviceId);
    }

    /**
     * Add a handler to process messages.
     *
     * @param handler handler to add
     */
    void addHandler(YamahaMusiccastUdpMessageHandler handler);

    /**
     * Remove the handler to stop processing messages.
     *
     * @param handler handler to remove
     */
    void removeHandler(YamahaMusiccastUdpMessageHandler handler);
}
