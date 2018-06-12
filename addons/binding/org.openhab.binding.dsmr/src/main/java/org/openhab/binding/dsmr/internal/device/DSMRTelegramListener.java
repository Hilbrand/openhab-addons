/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramListener;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramParser;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortErrorEvent;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 *
 */
@NonNullByDefault
class DSMRTelegramListener implements P1TelegramListener, DSMRPortListener {

    private static final int _100 = 100;

    private final Logger logger = LoggerFactory.getLogger(DSMRTelegramListener.class);
    private final P1TelegramParser parser;

    /**
     * Whether or not the lenientMode is activated (less strict checking on valid data)
     */
    private boolean lenientMode = true;

    @Nullable
    private DSMRPortEventListener dsmrPortListener;

    public DSMRTelegramListener(String serialPort) {
        parser = new P1TelegramParser(true, this);
    }

    public void setDsmrPortListener(DSMRPortEventListener dsmrPortListener) {
        this.dsmrPortListener = dsmrPortListener;
    }

    @Override
    public void handleData(byte[] data) {
        parser.parseData(data, 0, data.length);
    }

    @Override
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent) {
        if (dsmrPortListener != null) {
            dsmrPortListener.handlePortErrorEvent(portEvent);
        }
        parser.reset();
    }

    private void handleTelegramReceived(List<CosemObject> cosemObjects, String telegramDetails) {
        if (dsmrPortListener != null) {
            dsmrPortListener.handleTelegramReceived(cosemObjects, telegramDetails);
        }
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
            handleTelegramReceived(cosemObjects, telegramState.stateDetails);
        } else {
            if (lenientMode) {
                // In lenient mode, still send Cosem Objects
                if (cosemObjects.isEmpty()) {
                    logger.warn("Did not receive anything at all in lenient mode");
                    // dsmrPortListener.p1TelegramReceived(cosemObjects, telegramState.stateDetails);
                } else {
                    logger.debug("Still handling CosemObjects in lenient mode");
                    // dsmrPortListener.p1TelegramReceived(cosemObjects, telegramState.stateDetails);
                }
            } else {
                // Parsing was incomplete, don't send CosemObjects
                logger.warn("Dropping {} CosemObjects due {}", cosemObjects.size(), telegramState.stateDetails);
                cosemObjects = Collections.EMPTY_LIST;
                // dsmrPortListener.p1TelegramReceived(Collections.EMPTY_LIST, telegramState.stateDetails);
            }
        }
    }
}
