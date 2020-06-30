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
package org.openhab.binding.gpio.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.gpio.internal.configuration.InputPinConfiguration;
import org.openhab.binding.gpio.internal.configuration.OutputPinConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

/**
 *
 * @author Anatol Ogorek - Initial contribution
 * @author Hilbrand Bouwkamp - Refactored mcp23017 binding into a generic gpio binding
 */
@NonNullByDefault
public class PinStateHolder<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<ChannelUID, ChannelPin<GpioPinDigitalInput>> inputPins = new HashMap<>();
    private final Map<ChannelUID, ChannelPin<GpioPinDigitalOutput>> outputPins = new HashMap<>();
    private final GpioController controller;
    private final HandlerProvider<T> handlerProvider;

    private @Nullable GpioProvider provider;

    public PinStateHolder(final GpioController controller, final HandlerProvider<T> handlerProvider) {
        this.controller = controller;
        this.handlerProvider = handlerProvider;
    }

    public void setProvider(final T configuration) throws UnsupportedBusNumberException, IOException {
        shutdown();
        this.provider = handlerProvider.getProvider(configuration);
    }

    public synchronized @Nullable ChannelPin<? extends GpioPinDigital> getGpioPin(final ChannelUID channelUID) {
        final ChannelPin<GpioPinDigitalInput> inputPin = inputPins.get(channelUID);

        return inputPin == null ? outputPins.get(channelUID) : inputPin;
    }

    public State getState(final ChannelUID channelUID) {
        final ChannelPin<? extends GpioPinDigital> pin = getGpioPin(channelUID);

        return pin == null ? UnDefType.UNDEF : getState(controller.isHigh(pin.pin), pin);
    }

    public State getEventState(final GpioPinDigitalStateChangeEvent event,
            final @Nullable ChannelPin<GpioPinDigitalInput> pin) {
        return pin == null ? UnDefType.UNDEF : getState(event.getState() == PinState.HIGH, pin);
    }

    private State getState(final boolean state, final ChannelPin<? extends GpioPinDigital> pin) {
        return OnOffType.from(state ^ pin.activeLow);
    }

    public synchronized State setState(final ChannelUID channelUID, final boolean state) {
        final ChannelPin<GpioPinDigitalOutput> outputPin = outputPins.get(channelUID);
        final State newState;

        if (outputPin == null) {
            logger.warn("Could not find pin to set state for channel {}", channelUID);
            newState = UnDefType.UNDEF;
        } else {
            controller.setState(state ^ outputPin.activeLow, outputPin.pin);
            newState = getState(channelUID);
        }
        logger.debug("Channel {} set to state: {}, new state:{}", channelUID, state, newState);
        return newState;
    }

    public boolean isAlive() throws IOException {
        return handlerProvider.isAlive();
    }

    public synchronized GpioPinDigitalInput initializeInputPin(final ChannelUID channelUID,
            final InputPinConfiguration configuration) {
        final String channelId = channelUID.getIdWithoutGroup();
        final Pin pin = handlerProvider.getPin(channelId);
        logger.debug("Initializing input pin for channel {}:{} {} with configuration: {}", channelUID, channelId,
                pin.getName(), configuration);
        final GpioPinDigitalInput gpioPin = controller.provisionDigitalInputPin(provider, pin, channelId,
                configuration.getPullMode());

        inputPins.put(channelUID, new ChannelPin<>(gpioPin, configuration.activeLow));
        return gpioPin;
    }

    public synchronized GpioPinDigitalOutput initializeOutputPin(final ChannelUID channelUID,
            final OutputPinConfiguration configuration) {
        logger.debug("Initializing output pin for channel {}", channelUID);
        final String channelId = channelUID.getIdWithoutGroup();
        final Pin pin = handlerProvider.getPin(channelId);
        final PinState pinState = PinState.getState(configuration.state);
        final GpioPinDigitalOutput gpioPin = controller.provisionDigitalOutputPin(provider, pin, channelId, pinState);
        logger.debug("Bound digital output for PIN: {}, channel: {}, pinState: {}", pin, channelId, pinState);
        outputPins.put(channelUID, new ChannelPin<>(gpioPin, configuration.active_low));
        return gpioPin;
    }

    /**
     *
     */
    public void shutdown() {
        unBindGpioPins();
        final GpioProvider provider = this.provider;

        if (provider != null) {
            try {
                provider.shutdown();
            } catch (final RuntimeException e) {
                logger.trace("Shutdown failed", e);
            }
        }
    }

    private synchronized void unBindGpioPins() {
        inputPins.entrySet().forEach(entry -> controller.unprovisionPin(entry.getValue().pin));
        inputPins.clear();

        outputPins.entrySet().forEach(entry -> controller.unprovisionPin(entry.getValue().pin));
        outputPins.clear();
    }

    public synchronized Optional<Entry<ChannelUID, ChannelPin<GpioPinDigitalInput>>> getChannelForInputPin(
            final GpioPinDigitalInput pin) {
        return inputPins.entrySet().stream().filter(entry -> entry.getValue().pin.equals(pin)).findAny();
    }

    public static class ChannelPin<P extends GpioPin> {
        public final P pin;
        public final boolean activeLow;

        public ChannelPin(final P pin, final boolean activeLow) {
            this.pin = pin;
            this.activeLow = activeLow;
        }

        /**
         * @return the pin
         */
        public P getPin() {
            return pin;
        }
    }
}
