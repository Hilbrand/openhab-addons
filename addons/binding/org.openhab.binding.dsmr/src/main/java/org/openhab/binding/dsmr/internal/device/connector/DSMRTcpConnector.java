/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.connector;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class DSMRTcpConnector extends DSMRBaseConnector {
    private static final long WAIT_FOR_DATA_MILLIS = 500;

    private final Logger logger = LoggerFactory.getLogger(DSMRTcpConnector.class);
    private final Socket socket;

    /**
     *
     */
    // private Thread thread;

    @Nullable
    private final ScheduledExecutorService scheduler;

    /**
     *
     */
    @Nullable
    private InputStream tcpInputStream;

    /**
     *
     */
    private boolean running;

    @Nullable
    private ScheduledFuture<?> scheduleTask;

    public DSMRTcpConnector(String address, int port, ScheduledExecutorService scheduler, DSMRConnectorListener portListener)
            throws UnknownHostException, IOException {
        super(portListener);
        this.scheduler = scheduler;
        socket = new Socket(address, port);
    }

    /**
     *
     */
    public void open() {
        if (tcpInputStream != null) {
            close();
        }
        try {
            tcpInputStream = socket.getInputStream();

            synchronized (socket) {
                scheduleTask = scheduler.scheduleWithFixedDelay(this::handleDataAvailable, 0, WAIT_FOR_DATA_MILLIS,
                        TimeUnit.MILLISECONDS);
            }
        } catch (IOException e) {
            logger.debug("IOException during open", e);
            dsmrPortListener.handlePortErrorEvent(DSMRConnectorErrorEvent.READ_ERROR);
        }
    }

    @Override
    public void close() {
        try {
            if (tcpInputStream != null) {
                tcpInputStream.close();
            }
        } catch (IOException e) {
            logger.debug("IOException during close", e);
        } finally {
            synchronized (socket) {
                running = false;
                scheduleTask.cancel(true);
                scheduleTask = null;
            }
            super.close();
            tcpInputStream = null;
        }

    }
}
