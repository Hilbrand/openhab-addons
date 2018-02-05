/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import org.openhab.binding.dsmr.internal.device.serial.DSMRPort;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortSettings;

/**
 * @author Hilbrand Bouwkamp - Initial contribution
 */
public class DSMRFixedConfigDevice implements DSMRDevice {

    private final DSMRTelegramHandler handler;
    private final DSMRPort dsmrPort;
    private final DSMRPortSettings fixedPortSettings;

    public DSMRFixedConfigDevice(String serialPort, DSMRPortSettings fixedPortSettings,
            DSMRPortEventListener listener) {
        this.fixedPortSettings = fixedPortSettings;
        handler = new DSMRTelegramHandler(serialPort);
        handler.setDsmrPortListener(listener);
        dsmrPort = new DSMRPort(serialPort, true, handler);
    }

    @Override
    public void dispose() {
        stop();
        handler.dispose();
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
