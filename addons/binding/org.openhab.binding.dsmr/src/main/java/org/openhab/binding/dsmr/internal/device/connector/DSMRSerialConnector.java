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
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.DSMRBindingConstants;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DSMRSerialConnector extends DSMRBaseConnector implements SerialPortEventListener {
    private static final int _1000 = 1000;

    private final Logger logger = LoggerFactory.getLogger(DSMRSerialConnector.class);

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
     * DSMR Port listener
     */
    private final DSMRPortListener dsmrPortListener;

    /**
     * The portLock is used for the shared data used when opening and closing
     * the port. The following shared data must be guarded by the lock:
     * SerialPort, BufferedReader
     */
    private final Object portLock = new Object();

    /**
     * Creates a new DSMRPort. This is only a reference to a port. The port will
     * not be opened nor it is checked if the DSMR Port can successfully be
     * opened.
     *
     * @param serialPortName
     *            Device identifier of the post (e.g. /dev/ttyUSB0)
     *
     */
    public DSMRSerialConnector(String serialPortName, DSMRPortListener dsmrPortListener) {
        super(dsmrPortListener);
        this.portName = serialPortName;
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
     * parameters they're are not configurable.
     * <p>
     * If there are problem while opening the port, it is the responsibility of
     * the calling method to handle this situation (and for example close the
     * port again).
     *
     * @param portSettings The serial port settings to open the port with
     */
    public void open(DSMRSerialSettings portSettings) {
        synchronized (portLock) {
            try {
                logger.trace("Opening port {}", portName);
                // Opening Operating System Serial Port
                CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
                serialPort = portIdentifier.open(DSMRBindingConstants.DSMR_PORT_NAME,
                        (int) TimeUnit.SECONDS.toMillis(DSMRDeviceConstants.SERIAL_PORT_READ_TIMEOUT_SECONDS));

                // Configure Serial Port based on specified port speed
                logger.trace("Configure serial port parameters: {}", portSettings);
                serialPort.setSerialPortParams(portSettings.getBaudrate(), portSettings.getDataBits(),
                        portSettings.getStopbits(), portSettings.getParity());

                // SerialPort is ready, open the reader
                logger.trace("SerialPort opened successful on {}", portName);
                try {
                    open(serialPort.getInputStream());
                } catch (IOException ioe) {
                    logger.debug("Failed to get inputstream for serialPort. Closing port", ioe);
                    dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.READ_ERROR);
                    return;
                }

                try {
                    serialPort.addEventListener(this);
                } catch (TooManyListenersException tmle) {
                    logger.warn("DSMR binding will not be operational.", tmle);
                    return;
                }

                // Start listening for events
                serialPort.notifyOnDataAvailable(true);
                serialPort.notifyOnBreakInterrupt(true);
                serialPort.notifyOnFramingError(true);
                serialPort.notifyOnOverrunError(true);
                serialPort.notifyOnParityError(true);

                // serialPort.disableReceiveTimeout();
                serialPort.enableReceiveThreshold(_1000);
                serialPort.enableReceiveTimeout(_1000);
                // The binding is ready, let the meter know we want to receive values
                serialPort.setRTS(true);

                logger.info("Opened port {} with settings: {}", this.portName, portSettings);
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
     * Closes the DSMRPort and release OS resources.
     */
    @Override
    public void close() {
        synchronized (portLock) {
            logger.debug("Closing DSMR serial port");

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
            super.close();
            serialPort = null;
        }
    }

    public void setSerialPortParams(DSMRSerialSettings portSettings) {
        synchronized (portLock) {
            if (isOpen()) {
                logger.info("Update port {} with settings: {}", this.portName, portSettings);
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
                restart(portSettings);
            }
        }
    }

    /**
     * Switch the Serial Port speed (LOW --> HIGH and vice versa).
     */
    public void restart(DSMRSerialSettings portSettings) {
        synchronized (portLock) {
            logger.trace("Restart port {} with settings: {}", this.portName, portSettings);
            close();
            open(portSettings);
        }
    }

    @Override
    public void serialEvent(@Nullable SerialPortEvent seEvent) {
        if (seEvent == null) {
            return;
        }
        if (logger.isTraceEnabled() && SerialPortEvent.DATA_AVAILABLE != seEvent.getEventType()) {
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
        if (isOpen() && portEvent.getNewValue()) {
            logger.debug("New DSMR port {} event", typeName);
            dsmrPortListener.handlePortErrorEvent(DSMRPortErrorEvent.READ_ERROR);
        }
    }

    @Override
    protected void handleDataAvailable() {
        // open port if it is not open
        if (serialPort == null) {
            logger.warn("DSMRPort is not open, no values will be read");
            return;
        }
        super.handleDataAvailable();
    }
}
