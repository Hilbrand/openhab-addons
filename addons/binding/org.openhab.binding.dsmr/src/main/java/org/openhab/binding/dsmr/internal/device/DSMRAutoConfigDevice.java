/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.internal.device;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPort;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortErrorEvent;
import org.openhab.binding.dsmr.internal.device.serial.DSMRPortSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Simplified everything. Moved code around.
 */
@NonNullByDefault
public class DSMRAutoConfigDevice implements DSMRDevice, DSMRPortEventListener {
    private final Logger logger = LoggerFactory.getLogger(DSMRAutoConfigDevice.class);

    /*
     * Februari 2017
     * Due to the Dutch Smart Meter program where every residence is provided
     * a smart for free and the meters are DSMR V4 or higher
     * we assume the majority of meters communicate with HIGH_SPEED_SETTINGS
     * For older meters this means initializing is taking probably 1 minute
     */
    private final DSMRPortSettings DEFAULT_PORT_SETTINGS = DSMRPortSettings.HIGH_SPEED_SETTINGS;

    /**
     * Detector state
     */
    private enum DeviceConfigState {
        /**
         *
         */
        DETECTING_SETTINGS,
        /**
         *
         */
        SWITCHING_BAUDRATE,
        /**
         *
         */
        NORMAL,
        /**
         *
         */
        ERROR
    }

    /**
     * The port name
     */
    private final String portName;

    /**
     * Listener for discovered devices
     */
    private final DSMRPortEventListener portEventListener;

    /**
    *
    */
    private final ScheduledExecutorService scheduler;

    /**
     * DSMR Port instance
     */
    private final DSMRPort dsmrPort;

    /**
     * Timer for handling discovery half time
     */
    @Nullable
    private ScheduledFuture<?> halfTimeTimer;

    /**
     * Timer for handling end of discovery
     */
    @Nullable
    private ScheduledFuture<?> endTimeTimer;

    /**
    *
    */
    private DeviceConfigState state = DeviceConfigState.NORMAL;

    private DSMRPortSettings portSettings;

    private final DSMRTelegramListener parentlistener;

    private int receivedTimeoutSeconds;

    /**
     * Creates a new {@link DSMRAutoConfigDevice}
     *
     * @param serialPort the port name (e.g. /dev/ttyUSB0 or COM1)
     * @param parentlistener the {@link DSMRPortEventListener}
     * @param scheduler
     */
    public DSMRAutoConfigDevice(String serialPort, DSMRPortEventListener listener, ScheduledExecutorService scheduler,
            int receivedTimeoutSeconds) {
        this.receivedTimeoutSeconds = receivedTimeoutSeconds;
        parentlistener = new DSMRTelegramListener(serialPort);
        dsmrPort = new DSMRPort(serialPort, true, parentlistener);
        portName = dsmrPort.getPortName();
        this.portEventListener = listener;
        this.scheduler = scheduler;
        portSettings = DEFAULT_PORT_SETTINGS;
    }

    /**
     * Start the discovery process.
     */
    @Override
    public void start() {
        portSettings = DEFAULT_PORT_SETTINGS;
        setDetectSetings();
        logger.debug("[{}] Start detecting port settings.", portName);
        dsmrPort.open(portSettings);
        restartHalfTimer();
        endTimeTimer = scheduler.schedule(this::stop,
                receivedTimeoutSeconds * 4 /* DSMRDeviceConstants.DSMR_DISCOVERY_TIMEOUT_SECONDS */, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void restart() {
        logger.debug("Restart: state:{}", state);
        if (endTimeTimer == null || endTimeTimer.isDone()) {
            if (state == DeviceConfigState.ERROR) {
                // did receive anything but was an error.
                stop();
                wait(10);
                start();
            } else {
                setDetectSetings();
                dsmrPort.restart(portSettings);
            }
        }
    }

    private void setDetectSetings() {
        state = DeviceConfigState.DETECTING_SETTINGS;
        parentlistener.setDsmrPortListener(this);
    }

    private void wait(int waitInSeconds) {
        try {
            super.wait(TimeUnit.SECONDS.toMillis(waitInSeconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     *
     */
    @Override
    public synchronized void stop() {
        dsmrPort.close();
        if (state == DeviceConfigState.DETECTING_SETTINGS) {
            state = DeviceConfigState.ERROR;
        } else {
            state = DeviceConfigState.NORMAL;
        }
        logger.trace("stopped with state:{}", state);
        stopDetecting(state);
    }

    /**
     *
     * @param state
     */
    private void stopDetecting(DeviceConfigState state) {
        logger.debug("[{}] Stop detecting port settings.", portName);
        parentlistener.setDsmrPortListener(portEventListener);
        if (halfTimeTimer != null) {
            halfTimeTimer.cancel(true);
            halfTimeTimer = null;
        }
        if (endTimeTimer != null) {
            endTimeTimer.cancel(true);
            endTimeTimer = null;
        }
        this.state = state;
    }

    /**
     * Handle if telegrams are received.
     *
     * If there are cosem objects received a new bridge will we discovered
     *
     * @param cosemObjects list of {@link CosemObject}
     * @param stateDetails the details of the received telegram (this parameter is ignored)
     */
    @Override
    public void handleTelegramReceived(List<CosemObject> cosemObjects, String stateDetails) {
        logger.debug("[{}] Received {} cosemObjects, state:{}", portName, cosemObjects.size(), state);
        if (!cosemObjects.isEmpty()) {
            stopDetecting(DeviceConfigState.NORMAL);
            portEventListener.handleTelegramReceived(cosemObjects, stateDetails);
        }
    }

    /**
     * Event handler for DSMR Port events
     *
     * @param portEvent {@link DSMRPortErrorEvent} to handle
     */
    @Override
    public void handlePortErrorEvent(DSMRPortErrorEvent portEvent) {
        if (state == DeviceConfigState.SWITCHING_BAUDRATE) {
            logger.debug("[{}] Received portEvent during switching baudrate {}", portName, portEvent);
            return;
        }
        logger.trace("[{}] Received portEvent {}", portName, portEvent.getEventDetails());
        switch (portEvent) {
            case DONT_EXISTS: // Port does not exists (unexpected, since it was there, so port is not usable)
            case IN_USE: // Port is in use
            case NOT_COMPATIBLE: // Port not compatible
                handleError(portEvent);
                break;
            case READ_ERROR: // read error(try switching port speed)
                switchBaudrate();
                break;
            default:
                // Unknown event, log and do nothing
                logger.warn("Unknown event {}", portEvent);
                break;

        }
        // }
    }

    /**
     * Called when an event was triggered that could
     *
     * @param portEvent
     */
    private void handleError(DSMRPortErrorEvent portEvent) {
        logger.debug("[{}] Error during detecting port settings: {}, current state:{}.", portName,
                portEvent.getEventDetails(), state);
        stopDetecting(DeviceConfigState.ERROR);
        portEventListener.handlePortErrorEvent(portEvent);
    }

    /**
    *
    */
    private synchronized void switchBaudrate() {
        if (state == DeviceConfigState.DETECTING_SETTINGS) {
            restartHalfTimer();
            state = DeviceConfigState.SWITCHING_BAUDRATE;
            // wait(5);
            logger.debug(
                    "[{}] Detecting port settings is running for half time now and still nothing discovered, switching baudrate and retrying",
                    portName);
            portSettings = portSettings == DSMRPortSettings.HIGH_SPEED_SETTINGS ? DSMRPortSettings.LOW_SPEED_SETTINGS
                    : DSMRPortSettings.HIGH_SPEED_SETTINGS;
            dsmrPort.restart(portSettings);
            state = DeviceConfigState.DETECTING_SETTINGS;
        }
    }

    private void restartHalfTimer() {
        if (halfTimeTimer != null) {
            halfTimeTimer.cancel(true);
        }
        halfTimeTimer = scheduler.schedule(this::switchBaudrate, receivedTimeoutSeconds, TimeUnit.SECONDS);
    }
}
