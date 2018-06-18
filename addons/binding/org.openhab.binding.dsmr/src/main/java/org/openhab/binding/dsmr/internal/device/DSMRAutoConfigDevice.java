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
import org.openhab.binding.dsmr.internal.device.connector.DSMRConnectorErrorEvent;
import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialConnector;
import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialSettings;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - New class. contains code specific to detecting serial configuration.
 */
@NonNullByDefault
public class DSMRAutoConfigDevice implements DSMRDevice, DSMREventListener {

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
        NORMAL,
        /**
         *
         */
        ERROR
    }

    private static final long SWITCHING_BAUDRATE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    /*
     * Februari 2017
     * Due to the Dutch Smart Meter program where every residence is provided
     * a smart for free and the meters are DSMR V4 or higher
     * we assume the majority of meters communicate with HIGH_SPEED_SETTINGS
     * For older meters this means initializing is taking probably 1 minute
     */
    private static final DSMRSerialSettings DEFAULT_PORT_SETTINGS = DSMRSerialSettings.HIGH_SPEED_SETTINGS;

    private final Logger logger = LoggerFactory.getLogger(DSMRAutoConfigDevice.class);

    /**
     * The port name
     */
    private final String portName;

    /**
     * DSMR Port instance
     */
    private final DSMRSerialConnector dsmrPort;

    /**
    *
    */
    private final ScheduledExecutorService scheduler;

    private final int receivedTimeoutSeconds;

    private DSMRSerialSettings portSettings;

    /**
    *
    */
    private DeviceConfigState state = DeviceConfigState.NORMAL;

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

    private final DSMRTelegramListener telegramListener;

    private DSMREventListener parentListener;

    private long lastSwitchedBaudrateNanos;

    /**
     * Creates a new {@link DSMRAutoConfigDevice}
     *
     * @param serialPort the port name (e.g. /dev/ttyUSB0 or COM1)
     * @param telegramListener the {@link DSMREventListener}
     * @param scheduler
     */
    public DSMRAutoConfigDevice(String serialPort, DSMREventListener listener, ScheduledExecutorService scheduler,
            int receivedTimeoutSeconds) {
        this.parentListener = listener;
        this.scheduler = scheduler;
        this.receivedTimeoutSeconds = receivedTimeoutSeconds;
        telegramListener = new DSMRTelegramListener();
        telegramListener.setDsmrPortListener(listener);
        portSettings = DEFAULT_PORT_SETTINGS;
        dsmrPort = new DSMRSerialConnector(serialPort, telegramListener);
        portName = dsmrPort.getPortName();
    }

    @Override
    public void start() {
        logger.debug("[{}] Start detecting port settings.", portName);
        stopDetecting(DeviceConfigState.DETECTING_SETTINGS);
        portSettings = DEFAULT_PORT_SETTINGS;
        telegramListener.setDsmrPortListener(this);
        dsmrPort.open(portSettings);
        restartHalfTimer();
        endTimeTimer = scheduler.schedule(this::stopEndScheduler,
                receivedTimeoutSeconds * 4 /* DSMRDeviceConstants.DSMR_DISCOVERY_TIMEOUT_SECONDS */, TimeUnit.SECONDS);
    }

    @Override
    public void restart() {
        if (state == DeviceConfigState.ERROR) {
            stop();
            start();
        } else if (state == DeviceConfigState.NORMAL) {
            dsmrPort.restart(portSettings);
        }
    }

    @Override
    public synchronized void stop() {
        dsmrPort.close();
        stopDetecting(state);
        logger.trace("stopped with state:{}", state);
    }

    /**
     * Handle if telegrams are received.
     *
     * If there are cosem objects received a new bridge will we discovered
     *
     * @param cosemObjects list of {@link CosemObject}
     * @param telegramDetails the details of the received telegram (this parameter is ignored)
     */
    @Override
    public void handleTelegramReceived(List<CosemObject> cosemObjects, String telegramDetails) {
        logger.debug("[{}] Received {} cosemObjects, state:{}", portName, cosemObjects.size(), state);
        if (!cosemObjects.isEmpty()) {
            stopDetecting(DeviceConfigState.NORMAL);
            parentListener.handleTelegramReceived(cosemObjects, telegramDetails);
        }
    }

    /**
     * Event handler for DSMR Port events
     *
     * @param portEvent {@link DSMRConnectorErrorEvent} to handle
     */
    @Override
    public void handlePortErrorEvent(DSMRConnectorErrorEvent portEvent) {
        logger.trace("[{}] Received portEvent {}", portName, portEvent.getEventDetails());
        if (portEvent == DSMRConnectorErrorEvent.READ_ERROR) {
            switchBaudrate();
        } else {
            logger.debug("[{}] Error during detecting port settings: {}, current state:{}.", portName,
                    portEvent.getEventDetails(), state);
            stopDetecting(DeviceConfigState.ERROR);
            parentListener.handlePortErrorEvent(portEvent);
        }
    }

    /**
    *
    */
    private void switchBaudrate() {
        if (lastSwitchedBaudrateNanos + SWITCHING_BAUDRATE_TIMEOUT_NANOS > System.nanoTime()) {
            // Ignore switching baudrate if this is called within the timeout after a previous switch.
            return;
        }
        lastSwitchedBaudrateNanos = System.nanoTime();
        if (state == DeviceConfigState.DETECTING_SETTINGS) {
            restartHalfTimer();
            logger.debug(
                    "[{}] Detecting port settings is running for half time now and still nothing discovered, switching baudrate and retrying",
                    portName);
            portSettings = portSettings == DSMRSerialSettings.HIGH_SPEED_SETTINGS
                    ? DSMRSerialSettings.LOW_SPEED_SETTINGS
                    : DSMRSerialSettings.HIGH_SPEED_SETTINGS;
            dsmrPort.setSerialPortParams(portSettings);
        }
    }

    private void stopEndScheduler() {
        stopDetecting(state == DeviceConfigState.NORMAL ? DeviceConfigState.NORMAL : DeviceConfigState.ERROR);
    }

    /**
     *
     * @param state
     */
    private void stopDetecting(DeviceConfigState state) {
        telegramListener.setDsmrPortListener(parentListener);
        logger.debug("[{}] Stop detecting port settings.", portName);
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

    private void restartHalfTimer() {
        if (halfTimeTimer != null) {
            halfTimeTimer.cancel(true);
        }
        halfTimeTimer = scheduler.schedule(this::switchBaudrate, receivedTimeoutSeconds, TimeUnit.SECONDS);
    }

}
