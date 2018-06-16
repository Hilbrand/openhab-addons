/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dsmr.internal.device.connector.DSMRTcpConnector;

/**
 *
 * @author hilbrand
 */
@NonNullByDefault
public class DSMRTcpDevice implements DSMRDevice {

    private final DSMRTcpConnector connector;

    public DSMRTcpDevice(String ipAddress, int ipPort, ScheduledExecutorService scheduler,
            DSMRPortEventListener portListener) throws UnknownHostException, IOException {
        DSMRTelegramListener telegramListener = new DSMRTelegramListener();
        telegramListener.setDsmrPortListener(portListener);
        connector = new DSMRTcpConnector(ipAddress, ipPort, scheduler, telegramListener);
    }

    @Override
    public void restart() {
        start();
    }

    @Override
    public void start() {
        connector.open();
    }

    @Override
    public void stop() {
        connector.close();
    }

}
