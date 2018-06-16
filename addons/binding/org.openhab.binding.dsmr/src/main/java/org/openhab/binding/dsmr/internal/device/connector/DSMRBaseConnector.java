/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.connector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hilbrand
 */
@NonNullByDefault
class DSMRBaseConnector {

    private final Logger logger = LoggerFactory.getLogger(DSMRBaseConnector.class);

    protected final DSMRConnectorListener dsmrPortListener;

    /**
     * 1Kbyte buffer for storing data
     */
    private final byte[] buffer = new byte[1024]; // 1K

    private final Object readLock = new Object();

    private boolean open;
    private int bytesRead;
    private int msgCount;
    private int readCount;

    public DSMRBaseConnector(DSMRConnectorListener dsmrPortListener) {
        this.dsmrPortListener = dsmrPortListener;
    }

    /**
     * Input stream reading the Serial port
     */
    @Nullable
    private BufferedInputStream serialInputStream;

    protected void open(InputStream inputStream) {
        serialInputStream = new BufferedInputStream(inputStream);
        open = true;
    }

    protected boolean isOpen() {
        return open;
    }

    protected void close() {
        open = false;
        synchronized (readLock) {
            logger.trace("Total read {} messages with {} bytes in {} times ", msgCount, bytesRead, readCount);
            msgCount = bytesRead = readCount = 0;
        }

        if (serialInputStream != null) {
            try {
                serialInputStream.close();
            } catch (IOException ioe) {
                logger.debug("Failed to close reader", ioe);
            }
        }
        serialInputStream = null;
    }

    /**
     * Start reading from the DSMR port.
     *
     * @return {@link DeviceStateDetail} containing the details about the DeviceState
     */
    protected void handleDataAvailable() {
        try {
            synchronized (readLock) {
                // Read without lock on purpose to permit fast closure
                // This could lead to a NPE on variable serialInputStream so we will catch the NPE
                int bytesAvailable = serialInputStream.available();
                int rb = 0;
                int rc = 0;
                while (bytesAvailable > 0) {
                    int bytesAvailableRead = serialInputStream.read(buffer, 0, Math.min(bytesAvailable, buffer.length));
                    rb += bytesAvailableRead;
                    rc++;
                    if (open && bytesAvailableRead > 0) {
                        dsmrPortListener.handleData(buffer, bytesAvailableRead);
                    } else {
                        logger.debug("Expected bytes {} to read, but {} bytes were read", bytesAvailable,
                                bytesAvailableRead);
                    }
                    bytesAvailable = serialInputStream.available();
                }
                readCount += rc;
                this.bytesRead += rb;
                msgCount++;
            }
        } catch (IOException e) {
            dsmrPortListener.handlePortErrorEvent(DSMRConnectorErrorEvent.READ_ERROR);
            logger.debug("Exception on read port", e);
        } catch (NullPointerException e) {
            logger.trace("Port closed during read.", e);
        }
    }
}
