/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.pigpio.internal.handler;

import static org.openhab.binding.pigpio.internal.GPIOBindingConstants.GROUP_IN;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.pigpio.internal.HandlerProvider;
import org.openhab.binding.pigpio.internal.PinStateHolder;
import org.openhab.binding.pigpio.internal.PinStateHolder.ChannelPin;
import org.openhab.binding.pigpio.internal.configuration.InputPinConfiguration;
import org.openhab.binding.pigpio.internal.configuration.OutputPinConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

/**
 *
 * @author Anatol Ogorek - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored mcp23017 binding into a generic gpio binding
 */
@NonNullByDefault
public class DigitalPinHandler<T extends @Nullable GpioConfiguration> extends BaseThingHandler implements GpioPinListenerDigital {

    private final Logger logger = LoggerFactory.getLogger(DigitalPinHandler.class);
    private final Class<T> configurationClass;
    private final PinStateHolder<T> pinStateHolder;
    private @NonNullByDefault({}) T configuration;
    private @Nullable ScheduledFuture<?> refreshFuture;

    public DigitalPinHandler(final Thing thing, final GpioController controller,
            final HandlerProvider<T> handlerProvider) {
        super(thing);
        pinStateHolder = new PinStateHolder<T>(controller, handlerProvider);
        configurationClass = handlerProvider.getConfigurationClass();
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(configurationClass);
        initDevice();
        if (configuration.refresh > 0) {
            // no refresh is refresh rate is 0
            refreshFuture = scheduler.scheduleWithFixedDelay(this::refresh, configuration.refresh, configuration.refresh, TimeUnit.SECONDS);
        }
    }

    public void refresh() {
        boolean restart = false;
        try {
            if (pinStateHolder.isAlive()) {
                for (final Channel channel : getThing().getChannels()) {
                    final ChannelUID uid = channel.getUID();

                    if (isLinked(uid)) {
                        handleRefresh(uid);
                    }
                }
            } else {
                logger.debug("Thing {} seems not reachable, trying to restart.", getThing().getUID());
                restart = true;
            }
        } catch (final IOException e) {
            logger.debug("IOException, trying to restart {}", getThing().getUID(), e);
            restart = true;
        }
        if (restart) {
            initDevice();
        }
    }

    private void initDevice() {
        try {
            synchronized (pinStateHolder) {
                pinStateHolder.setProvider(configuration);
            }
        } catch (final UnsupportedBusNumberException e) {
            logger.debug("UnsupportedBusNumberException", e);
            final String message = String.format("Could not connect to %s."
                    + ". Either it's not accessible or the user running openHAB is not part of the group `i2c and `gpio`",
                    getThing().getUID());

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
        } catch (final IOException e) {
            logger.debug("IOException", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            return;
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (command instanceof RefreshType) {
            handleRefresh(channelUID);
        } else if (command instanceof OnOffType) {
            handleOnOffCommand(channelUID, (OnOffType) command);
        }
    }

    private void handleRefresh(final ChannelUID channelUID) {
        updateState(channelUID, pinStateHolder.getState(channelUID));
    }

    private void handleOnOffCommand(final ChannelUID channelUID, final OnOffType command) {
        pinStateHolder.setState(channelUID, command == OnOffType.ON);
        handleRefresh(channelUID);
    }

    @Override
    public void dispose() {
        pinStateHolder.shutdown();
        final ScheduledFuture<?> future = refreshFuture;

        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void channelLinked(final ChannelUID channelUID) {
        final ChannelPin<?> gpioPin = pinStateHolder.getGpioPin(channelUID);
        final Channel channel = getThing().getChannel(channelUID.getId());

        logger.debug("Initialize pin for channel {}: {}", channelUID, gpioPin);
        if (gpioPin != null || channel == null) {
            logger.warn("Unable to link channel {} to a pin", channelUID);
        } else {
            final Configuration channelConfig = channel.getConfiguration();
            final String groupId = channelUID.getGroupId();

            if (GROUP_IN.equals(groupId)) {
                final GpioPinDigitalInput inputPin = pinStateHolder.initializeInputPin(channelUID,
                        channelConfig.as(InputPinConfiguration.class));
                inputPin.addListener(this);
            } else {
                pinStateHolder.initializeOutputPin(channelUID, channelConfig.as(OutputPinConfiguration.class));
            }
            handleRefresh(channelUID);
            super.channelLinked(channelUID);
        }
    }

    @Override
    public void channelUnlinked(final ChannelUID channelUID) {
        logger.info("Channel {} unlinked", channelUID);
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(final @Nullable GpioPinDigitalStateChangeEvent event) {
        if (event == null) {
            return;
        }
        logger.debug("{}: Input event for pin {}: {}", getThing().getUID(), event.getPin(), event.getState());
        final Optional<Entry<ChannelUID, ChannelPin<GpioPinDigitalInput>>> channelForPin = pinStateHolder
                .getChannelForInputPin((GpioPinDigitalInput) event.getPin());

        if (channelForPin.isPresent()) {
            channelForPin.ifPresent(pin -> updateState(pin.getKey(),
                    pinStateHolder.getEventState(event, (ChannelPin<GpioPinDigitalInput>) pin.getValue())));
        } else {
            logger.debug("Could not find input channel for pin: {}", event.getPin());
        }
    }
}
