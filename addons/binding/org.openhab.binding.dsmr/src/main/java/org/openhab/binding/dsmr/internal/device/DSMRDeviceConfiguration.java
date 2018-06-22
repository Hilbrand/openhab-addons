/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import org.apache.commons.lang.StringUtils;

/**
 * Class described the DSMRDeviceConfiguration.
 *
 * This class is supporting the Configuration.as functionality from {@link Configuration}
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - added refresh and serial-over-ip configuration
 */
public class DSMRDeviceConfiguration {
    /**
     * Serial port name
     */
    public String serialPort;

    /**
     * Serial port baud rate
     */
    public int serialPortBaudrate;

    /**
     * Serial port data bits
     */
    public int serialPortDatabits;

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
     * IP address for serial-over-ip communication. (No rfc2217, but streaming of serial data over ip).
     */
    public String ipAddress;

    /**
     * IP port for serial-over-ip communication. (No rfc2217, but streaming of serial data over ip).
     */
    public int ipPort;

    /**
     * When no message was received after the configured number of seconds action will be taken.
     */
    public int receivedTimeout;

    /**
     * @return true is serial port speed should be detected automatically.
     */
    public boolean isSerialAutoDetection() {
        return serialPortEnableAutoDetection;
    }

    /**
     * @return true if serial port settings are all set and no auto detecting is enabled.
     */
    public boolean isSerialFixedSettings() {
        return !isSerialAutoDetection() && serialPortBaudrate > 0 && serialPortDatabits > 0
                && !StringUtils.isBlank(serialPortParity) && !StringUtils.isBlank(serialPortStopbits);
    }

    /**
     * @return true if ip address and port are set by the user.
     */
    public boolean isTcpSettings() {
        return ipAddress != null && ipPort > 0;
    }

    @Override
    public String toString() {
        return "DSMRDeviceConfiguration [serialPort=" + serialPort + ", serialPortBaudrate=" + serialPortBaudrate
                + ", serialPortDatabits=" + serialPortDatabits + ", serialPortParity=" + serialPortParity
                + ", serialPortStopbits=" + serialPortStopbits + ", serialPortEnableAutoDetection="
                + serialPortEnableAutoDetection + ", ipAddress=" + ipAddress + ", ipPort=" + ipPort
                + ", receivedTimeout=" + receivedTimeout + "]";
    }
}
