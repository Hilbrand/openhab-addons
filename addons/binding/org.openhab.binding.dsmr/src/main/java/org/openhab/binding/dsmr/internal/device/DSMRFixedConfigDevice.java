/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialConnector;
import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialSettings;

/**
 * Implementation of a DSMRDevice with fixed serial port settings. With fixed port settings the code is much simpler
 * because no detecting of settings needs to be done and when things fail no redirecting is needed either.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
public class DSMRFixedConfigDevice implements DSMRDevice {

    private final DSMRTelegramListener telegramListener;
    private final DSMRSerialConnector dsmrPort;
    private final DSMRSerialSettings fixedPortSettings;

    public DSMRFixedConfigDevice(String serialPort, DSMRSerialSettings fixedPortSettings,
            DSMRPortEventListener listener) {
        this.fixedPortSettings = fixedPortSettings;
        telegramListener = new DSMRTelegramListener();
        telegramListener.setDsmrPortListener(listener);
        dsmrPort = new DSMRSerialConnector(serialPort, telegramListener);
    }

    @Override
    public void start() {
        dsmrPort.open(fixedPortSettings);
    }

    @Override
    public void restart() {
        dsmrPort.restart(fixedPortSettings);
    }

    @Override
    public void stop() {
        dsmrPort.close();
    }
}
