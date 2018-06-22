/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.internal.device.connector.DSMRConnectorErrorEvent;
import org.openhab.binding.dsmr.internal.device.connector.DSMRConnectorListener;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramListener;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper listener to receive telegram data from the connector, send it to the parser and forward data or errors from
 * the parser to the DSMR Device.
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Moved this code out of the DSMRPort class, fixed some issues and reduced code
 */
@NonNullByDefault
class DSMRTelegramListener implements P1TelegramListener, DSMRConnectorListener {

    private final Logger logger = LoggerFactory.getLogger(DSMRTelegramListener.class);
    private final P1TelegramParser parser;

    @Nullable
    private DSMREventListener dsmrEventListener;

    /**
     * Constructor.
     */
    public DSMRTelegramListener() {
        parser = new P1TelegramParser(true, this);
    }

    /**
     * Set the DSMR event listener.
     *
     * @param eventListener the listener to set
     */
    public void setDsmrEventListener(DSMREventListener eventListener) {
        this.dsmrEventListener = eventListener;
    }

    @Override
    public void handleData(byte[] data, int length) {
        parser.parseData(data, 0, length);
    }

    @Override
    public void handleErrorEvent(DSMRConnectorErrorEvent portEvent) {
        if (dsmrEventListener != null) {
            dsmrEventListener.handleErrorEvent(portEvent);
        }
        parser.reset();
    }

    /**
     * Handler for cosemObjects received in a P1 telegram
     *
     * @param cosemObjects. List of received {@link CosemObject} objects
     * @param telegramState. {@link TelegramState} describing the state of the received telegram.
     */
    @Override
    public void telegramReceived(List<CosemObject> cosemObjects, TelegramState telegramState) {
        logger.debug("Received {} Cosem Objects, telegramState: {}", cosemObjects.size(), telegramState);
        if (telegramState == TelegramState.OK) {
            if (dsmrEventListener != null) {
                dsmrEventListener.handleTelegramReceived(cosemObjects, telegramState.stateDetails);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Telegram received with error state '{}': {}", telegramState,
                        cosemObjects.stream().map(CosemObject::toString).collect(Collectors.joining(",")));
            }
        }
    }
}
