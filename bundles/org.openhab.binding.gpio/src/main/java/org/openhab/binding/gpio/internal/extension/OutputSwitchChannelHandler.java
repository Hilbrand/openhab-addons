package org.openhab.binding.gpio.internal.extension;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

public class OutputSwitchChannelHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<ChannelUID, GpioPinDigitalOutput> outputPins = new HashMap<>();
    private final Map<ChannelUID, Boolean> outputActiveLow = new HashMap<>();
    private final GpioController gpioController;

    public OutputSwitchChannelHandler(final GpioController gpioController) {
        this.gpioController = gpioController;
    }

    // handle output channel
    public void handleOutputChannel(final ChannelUID channelUID, final Command command) {
        final GpioPinDigitalOutput outputPin = outputPins.get(channelUID);
        final boolean activeLowFlag = Boolean.TRUE.equals(outputActiveLow.get(channelUID));
        final PinState pinState = command == OnOffType.ON ^ activeLowFlag ? PinState.HIGH : PinState.LOW;

        logger.debug("got output pin {} for channel {} and command {} [active_low={}, new_state={}]", outputPin,
                channelUID, command, activeLowFlag, pinState);
        gpioController.setState(pinState, outputPin);
    }

}
