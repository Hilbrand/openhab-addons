/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.concurrent.TimeUnit;

/**
 * This class holds all the constants that are applicable for the DSMR Device
 *
 * @author M. Volaart - Initial contribution
 */
public class DSMRDeviceConstants {

    /**
     * Serial port read time out (15 seconds)
     */
    public static final long SERIAL_PORT_READ_TIMEOUT_SECONDS = 15;

    /**
     * Timeout for detecting the correct serial port settings
     */
    public static final long SERIAL_PORT_AUTO_DETECT_TIMEOUT_SECONDS = SERIAL_PORT_READ_TIMEOUT_SECONDS * 2;

    /**
     * Timeout for recovery from offline mode
     */
    public static final long RECOVERY_TIMEOUT_NANOS = TimeUnit.SECONDS
            .toNanos(SERIAL_PORT_AUTO_DETECT_TIMEOUT_SECONDS * 2);

    /**
     * Timeout for Discovery timeout
     */
    public final static int DSMR_DISCOVERY_TIMEOUT_SECONDS = 120;

}
