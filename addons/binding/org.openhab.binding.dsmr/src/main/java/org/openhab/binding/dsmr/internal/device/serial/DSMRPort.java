/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device.serial;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.DSMRBindingConstants;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * Class that implements the DSMR port functionality that comply to the Dutch Smart Meter Requirements.
 * <p>
 * This class will handle communication with the Serial Port itself and notify listeners about events and
 * received P1 telegrams
 * <p>
 * The read method will claim OS resources if necessary. If the read method
 * encounters unrecoverable problems it will automatically close itself
 * <p>
 * The close method can be called asynchronous and will release OS resources.
 * </p>
 *
 * @author M. Volaart - Initial contribution
 */
@NonNullByDefault
public class DSMRPort implements SerialPortEventListener {
    private static final int _1000 = 1000;

    private final Logger logger = LoggerFactory.getLogger(DSMRPort.class);

    /**
     * private object variables
     */
    private final String portName;

    /**
     * serial port instance
     */
    @Nullable
    private SerialPort serialPort;

    /**
     * Input stream reading the Serial port
     */
    @Nullable
    private BufferedInputStream serialInputStream;

    /**
     * 1Kbyte buffer for storing data
     */
    private final byte[] buffer = new byte[1024]; // 1K

    /**
     * DSMR Port listener
     */
    private final DSMRPortListener dsmrPortListener;

    /**
     * The portLock is used for the shared data used when opening and closing
     * the port. The following shared data must be guarded by the lock:
     * SerialPort, BufferedReader
     */
    private final Object portLock = new Object();
    private final Object readLock = new Object();

    private boolean open;
    private int bytesRead;
    private int msgCount;
    private int readCount;

    /**
     * Creates a new DSMRPort. This is only a reference to a port. The port will
     * not be opened nor it is checked if the DSMR Port can successfully be
     * opened.
     *
     * @param portName
     *            Device identifier of the post (e.g. /dev/ttyUSB0)
     *
     */
    public DSMRPort(String portName, boolean lenientMode, DSMRPortListener dsmrPortListener) {
        this.portName = portName;
        this.dsmrPortListener = dsmrPortListener;
    }

    public String getPortName() {
        return portName;
    }

    /**
     * Opens the Operation System Serial Port
     * <p>
     * This method opens the port and set Serial Port parameters according to
     * the DSMR specification. Since the specification is clear about these
     * parameters there are not configurable.
     * <p>
     * If there are problem while opening the port, it is the responsibility of
     * the calling method to handle this situation (and for example close the
     * port again).
     * <p>
     * Opening an already open port is harmless. The method will return
     * immediately
     *
     * @param portSettings
     *
     * @return {@link DeviceStateDetail} containing the details about the DeviceState
     */
    public void open(DSMRPortSettings portSettings) {
        synchronized (portLock) {
            try {
                logger.debug("Opening port {}", portName);
                // Opening Operating System Serial Port
                CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
                CommPort commPort = portIdentifier.open(DSMRBindingConstants.DSMR_PORT_NAME,
                        (int) TimeUnit.SECONDS.toMillis(DSMRDeviceConstants.SERIAL_PORT_READ_TIMEOUT_SECONDS));
                serialPort = (SerialPort) commPort;

                // Configure Serial Port based on specified port speed
                logger.debug("Configure serial port parameters: {}", portSettings);

                serialPort.setSerialPortParams(portSettings.getBaudrate(), portSettings.getDataBits(),
                        portSettings.getStopbits(), portSettings.getParity());

                // SerialPort is ready, open the reader
                logger.debug("SerialPort opened successful on {}", portName);
                try {
                    serialInputStream = new BufferedInputStream(serialPort.getInputStream());
                } catch (IOException ioe) {
                    logger.debug("Failed to get inputstream for serialPort. Closing port", ioe);

                    dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.READ_ERROR);
                }
                logger.info("DSMR Port opened successful with port settings: {}", portSettings);

                try {
                    serialPort.addEventListener(this);
                } catch (TooManyListenersException tmle) {
                    logger.warn("DSMR binding will not be operational.", tmle);
                }

                // Start listening for events
                serialPort.notifyOnDataAvailable(true);
                serialPort.notifyOnBreakInterrupt(true);
                serialPort.notifyOnFramingError(true);
                serialPort.notifyOnOverrunError(true);
                serialPort.notifyOnParityError(true);

                serialPort.enableReceiveTimeout(_1000);
                // The binding is ready, let the meter know we want to receive values
                serialPort.setRTS(true);

                open = true;
            } catch (NoSuchPortException nspe) {
                logger.debug("Port {} does not exists", portName, nspe);

                dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.DONT_EXISTS);
            } catch (PortInUseException piue) {
                logger.debug("Port already in use: {}", portName, piue);

                dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.IN_USE);
            } catch (UnsupportedCommOperationException ucoe) {
                logger.debug("Port does not support requested port settings (invalid dsmr:portsettings parameter?): {}",
                        portName, ucoe);

                dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.NOT_COMPATIBLE);
            }
        }
    }

    /**
     * Closes the DSMRPort and release OS resources
     *
     * The listener will be notified of the closed event. This event is sent while holding
     * the portLock ensuring correct order of handling events
     */
    public void close() {
        synchronized (portLock) {
            open = false;
            logger.trace("Total read {} messages with {} bytes in {} times ", msgCount, bytesRead, readCount);
            msgCount = bytesRead = readCount = 0;
            logger.debug("Closing DSMR port");

            // Stop listening for serial port events
            if (serialPort != null) {
                // Let meter stop sending values
                serialPort.setRTS(false);
                serialPort.removeEventListener();
                try {
                    serialPort.getInputStream().close();
                } catch (IOException ioe) {
                    logger.debug("Failed to close serial port inputstream", ioe);
                }
                serialPort.close();
            }
            if (serialInputStream != null) {
                try {
                    serialInputStream.close();
                } catch (IOException ioe) {
                    logger.debug("Failed to close reader", ioe);
                }
            }

            // Release resources
            serialInputStream = null;
            serialPort = null;
        }
    }

    /**
     * Start reading from the DSMR port.
     *
     * @return {@link DeviceStateDetail} containing the details about the DeviceState
     */
    private void handleDataAvailable() {
        try {
            synchronized (readLock) {
                // open port if it is not open
                if (serialPort == null) {
                    logger.warn("DSMRPort is not open, no values will be read");
                    return;// DSMRPortEvent.CLOSED;
                }

                // Read without lock on purpose to permit fast closure
                // This could lead to a NPE on variable serialInputStream so we will catch the NPE
                int bytesAvailable = serialInputStream.available();
                int rb = 0;
                int rc = 0;
                while (bytesAvailable > 0) {
                    int bytesAvailableRead = serialInputStream.read(buffer, 0, Math.min(bytesAvailable, buffer.length));
                    rb += bytesAvailableRead;
                    rc++;
                    // logger.trace("Read #{} bytes.", bytesRead);
                    if (open && bytesAvailableRead > 0) {
                        dsmrPortListener.handleData(Arrays.copyOfRange(buffer, 0, bytesAvailableRead));
                    } else {
                        logger.debug("Expected bytes {} to read, but {} bytes were read", bytesAvailable,
                                bytesAvailableRead);
                    }
                    bytesAvailable = serialInputStream.available();
                }
                // logger.trace("Read #{} bytes {} times in this message", rb, rc);
                synchronized (portLock) {
                    readCount += rc;
                    this.bytesRead += rb;
                    msgCount++;
                }
            }
        } catch (IOException e) {
            dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.READ_ERROR);
            logger.debug("Exception on read port", e);
        } catch (NullPointerException e) {
            logger.trace("Port closed during read.");
        }
    }

    /**
     * Switch the Serial Port speed (LOW --> HIGH and vice versa).
     */
    public void restart(DSMRPortSettings portSettings) {
        synchronized (portLock) {
            if (open) {
                logger.info("Set port {} with settings: {}", this.portName, portSettings);
                try {
                    serialPort.setSerialPortParams(portSettings.getBaudrate(), portSettings.getDataBits(),
                            portSettings.getStopbits(), portSettings.getParity());
                } catch (UnsupportedCommOperationException e) {
                    logger.debug(
                            "Port does not support requested port settings (invalid dsmr:portsettings parameter?): {}",
                            portName, portSettings);
                    dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.NOT_COMPATIBLE);
                }
            } else {
                close();
                open(portSettings);
            }
        }
    }

    @Override
    public void serialEvent(@Nullable SerialPortEvent seEvent) {
        if (seEvent == null) {
            return;
        }
        if (SerialPortEvent.DATA_AVAILABLE != seEvent.getEventType()) {
            logger.trace("Serial event: {}, new value:{}", seEvent.getEventType(), seEvent.getNewValue());
        }
        try {
            switch (seEvent.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
                    handleDataAvailable();
                    break;
                case SerialPortEvent.BI:
                    handleErrorEvent("Break interrupt", seEvent);
                    break;
                case SerialPortEvent.FE:
                    handleErrorEvent("Frame error", seEvent);
                    break;
                case SerialPortEvent.OE:
                    handleErrorEvent("Overrun error", seEvent);
                    break;
                case SerialPortEvent.PE:
                    handleErrorEvent("Parity error", seEvent);
                    break;
                default: // do nothing
            }
        } catch (RuntimeException e) {
            logger.warn("RuntimeException during handling serial event: {}", seEvent.getEventType(), e);
        }
    }

    /**
     *
     * @param typeName
     * @param portEvent
     */
    private void handleErrorEvent(String typeName, SerialPortEvent portEvent) {
        if (open && portEvent.getNewValue()) {
            logger.debug("New DSMR port {} event", typeName);
            dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.READ_ERROR);
        }
    }
}
