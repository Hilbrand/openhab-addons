/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.dsmr.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.dsmr.internal.device.DSMRSerialAutoDevice;
import org.openhab.binding.dsmr.internal.device.DSMRDevice;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceConfiguration;
import org.openhab.binding.dsmr.internal.device.DSMRDeviceRunnable;
import org.openhab.binding.dsmr.internal.device.DSMREventListener;
import org.openhab.binding.dsmr.internal.device.DSMRFixedConfigDevice;
import org.openhab.binding.dsmr.internal.device.DSMRTcpDevice;
import org.openhab.binding.dsmr.internal.device.connector.DSMRConnectorErrorEvent;
import org.openhab.binding.dsmr.internal.device.connector.DSMRSerialSettings;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.meter.DSMRMeterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DSMRBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author M. Volaart - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored way messages are forwarded to meters. Removed availableMeters dependency.
 */
@NonNullByDefault
public class DSMRBridgeHandler extends BaseBridgeHandler implements DSMREventListener {

    /**
     * Factor that will be multiplied with {@link #receivedTimeoutNanos} to get the timeout factor after which the
     * device is set off line.
     */
    private static final int OFFLINE_TIMEOUT_FACTOR = 10;

    private final Logger logger = LoggerFactory.getLogger(DSMRBridgeHandler.class);

    /**
     * Additional meter listeners to get received meter values.
     */
    private final List<DSMRMeterListener> meterListeners = new ArrayList<>();
    /**
     * Long running process that controls the DSMR device connection.
     */
    @Nullable
    private DSMRDeviceRunnable dsmrDeviceRunnable;
    /**
     * Thread for {@link DSMRDeviceRunnable}. A thread is used because the {@link DSMRDeviceRunnable} is a blocking
     * process that runs as long as the thing is not disposed.
     */
    @Nullable
    private Thread dsmrDeviceThread;
    /**
     * Watchdog to check if messages received and restart if necessary.
     */
    @Nullable
    private ScheduledFuture<?> watchdog;
    /**
     * Number of nanoseconds after which a timeout is triggered when no messages received.
     */
    private long receivedTimeoutNanos;

    /**
     * Timestamp in nanoseconds of last P1 telegram received
     */
    private long telegramReceivedTimeNanos;

    /**
     * Constructor
     *
     * @param bridge the Bridge ThingType
     */
    public DSMRBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    /**
     * The DSMRBridgeHandler does not support handling commands.
     *
     * @param channelUID
     * @param command
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // DSMRBridgeHandler does not support commands
    }

    /**
     * Initializes this DSMRBridgeHandler
     *
     * This method will get the corresponding configuration and initialize and start the corresponding DSMRDevice.
     */
    @Override
    public void initialize() {
        DSMRDeviceConfiguration deviceConfig = getConfigAs(DSMRDeviceConfiguration.class);

        logger.trace("Using configuration {}", deviceConfig);
        updateStatus(ThingStatus.UNKNOWN);
        receivedTimeoutNanos = TimeUnit.SECONDS.toNanos(deviceConfig.receivedTimeout);
        DSMRDevice dsmrDevice = createDevice(deviceConfig);
        if (dsmrDevice == null) {
            logger.debug("Incomplete configuration: {}", deviceConfig);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Incomplete configuration. Not all required configuration settings are set.");
        } else {
            // Start time monitoring from now.
            resetLastReceivedState();
            dsmrDeviceRunnable = new DSMRDeviceRunnable(dsmrDevice, this);
            dsmrDeviceThread = new Thread(dsmrDeviceRunnable);
            dsmrDeviceThread.start();
            watchdog = scheduler.scheduleWithFixedDelay(this::alive, receivedTimeoutNanos, receivedTimeoutNanos,
                    TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Creates the {@link DSMRDevice} that corresponds with the user specified configuration.
     *
     * @param deviceConfig device configuration
     * @return Specific {@link DSMRDevice} instance or null if no valid configuration was set.
     */
    @Nullable
    private DSMRDevice createDevice(DSMRDeviceConfiguration deviceConfig) {
        DSMRDevice dsmrDevice;

        if (deviceConfig.isTcpSettings()) {
            try {
                dsmrDevice = new DSMRTcpDevice(deviceConfig.ipAddress, deviceConfig.ipPort, scheduler, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (deviceConfig.isSerialAutoDetection()) {
            dsmrDevice = new DSMRSerialAutoDevice(deviceConfig.serialPort, this, scheduler,
                    deviceConfig.receivedTimeout);
        } else if (deviceConfig.isSerialFixedSettings()) {
            dsmrDevice = new DSMRFixedConfigDevice(deviceConfig.serialPort,
                    DSMRSerialSettings.getPortSettingsFromConfiguration(deviceConfig), this);
        } else {
            dsmrDevice = null;
        }
        return dsmrDevice;
    }

    /**
     * Adds a meter discovery listener
     *
     * @param meterListener the meter discovery listener to add
     *
     * @return true if listener is added, false otherwise
     */
    public boolean registerDSMRMeterListener(DSMRMeterListener meterListener) {
        logger.debug("Register DSMRMeterListener");
        return meterListeners.add(meterListener);
    }

    /**
     * Removes a meter discovery listener
     *
     * @param meterListener the meter discovery listener to remove
     *
     * @return true is listener is removed, false otherwise
     */
    public boolean unregisterDSMRMeterListener(DSMRMeterListener meterListener) {
        logger.debug("Unregister DSMRMeterListener");
        return meterListeners.remove(meterListener);
    }

    /**
     * Watchdog method that is run with the scheduler and checks if meter values were received. If the timeout is
     * exceeded the device is restarted. If the off line timeout factor is exceeded the device is set off line. By not
     * setting the device on first exceed off line their is some slack in the system and it won't flip on and offline in
     * case of an unstable system.
     */
    private void alive() {
        logger.trace("Bridge alive check with #{} children.", getThing().getThings().size());
        long deltaLastReceived = System.nanoTime() - telegramReceivedTimeNanos;

        if (deltaLastReceived > receivedTimeoutNanos) {
            logger.debug("No data received for {} seconds, restarting port if possible.",
                    TimeUnit.NANOSECONDS.toSeconds(deltaLastReceived));
            if (dsmrDeviceRunnable != null) {
                dsmrDeviceRunnable.restart();
            }
            if (deltaLastReceived > receivedTimeoutNanos * OFFLINE_TIMEOUT_FACTOR) {
                logger.trace("Setting device offline if not yet done, and reset last received time.");
                if (getThing().getStatus() == ThingStatus.ONLINE) {
                    deviceOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Not receiving data from meter.");
                }
                resetLastReceivedState();
            }
        }
    }

    /**
     * Sets the last received time of messages to the current time.
     */
    private void resetLastReceivedState() {
        telegramReceivedTimeNanos = System.nanoTime();
        logger.trace("Telegram received time set: {}", telegramReceivedTimeNanos);
    }

    @Override
    public synchronized void handleTelegramReceived(List<CosemObject> cosemObjects, String telegramDetails) {
        if (cosemObjects.isEmpty()) {
            logger.debug("Parsing worked but something went wrong, so there were no CosemObjects:{}", telegramDetails);
            deviceOffline(ThingStatusDetail.COMMUNICATION_ERROR, telegramDetails);
        } else {
            resetLastReceivedState();
            meterValueReceived(new ArrayList<>(cosemObjects));
        }
    }

    @Override
    public void handleErrorEvent(DSMRConnectorErrorEvent portEvent) {
        if (portEvent != DSMRConnectorErrorEvent.READ_ERROR) {
            deviceOffline(ThingStatusDetail.CONFIGURATION_ERROR, portEvent.getEventDetails());
        }
    }

    /**
     * Method to forward the last received messages to the bound meters and to the meterListeners.
     *
     * @param lastMeterValues received meter values.
     */
    private void meterValueReceived(List<CosemObject> lastMeterValues) {
        updateStatus(ThingStatus.ONLINE);
        getThing().getThings().forEach(child -> {
            if (logger.isTraceEnabled()) {
                logger.trace("Update child:{} with {} objects", child.getThingTypeUID().getId(),
                        lastMeterValues.size());
            }
            if (child.getHandler() instanceof DSMRMeterHandler) {
                ((DSMRMeterHandler) child.getHandler()).meterValueReceived(lastMeterValues);
            }
        });
        meterListeners.forEach(m -> m.meterValueReceived(lastMeterValues));
    }

    /**
     * On dispose the DSMR device is removed
     */
    @Override
    public void dispose() {
        if (watchdog != null) {
            watchdog.cancel(true);
            watchdog = null;
        }
        if (dsmrDeviceRunnable != null) {
            dsmrDeviceRunnable.stop();
        }
    }

    /**
     * Convenience method to set device off line.
     *
     * @param status off line status
     * @param details off line detailed message
     */
    private void deviceOffline(ThingStatusDetail status, String details) {
        updateStatus(ThingStatus.OFFLINE, status, details);
    }
}
