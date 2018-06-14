/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

/**
 * Class described the DSMRDeviceConfiguration.
 *
 * This class is supporting the Configuration.as functionality from {@link Configuration}
 *
 * @author M. Volaart - Initial contribution
 */
public class DSMRDeviceConfiguration {
    /**
     * Serial port name
     */
    public String serialPort;

    /**
     * Serial port baud rate
     */
    public Integer serialPortBaudrate;

    /**
     * Serial port data bits
     */
    public Integer serialPortDatabits;

    /**
     * Serial port parity
     */
    public String serialPortParity;

    /**
     * Serial port stop bits
     */
    public String serialPortStopbits;

    /**
     * Serial port auto detection flag
     */
    public boolean serialPortEnableAutoDetection = true;

    /**
     * When no message was received after the configured number of seconds action will be taken.
     */
    public int receivedTimeout;

    @Override
    public String toString() {
        return "DSMRDeviceConfiguration(portName:" + serialPort + ", baudrate:" + serialPortBaudrate + ", data bits:"
                + serialPortDatabits + ", parity:" + serialPortParity + ", stop bits:" + serialPortStopbits
                + ", auto detection enabled:" + serialPortEnableAutoDetection + ")";
    }
}
