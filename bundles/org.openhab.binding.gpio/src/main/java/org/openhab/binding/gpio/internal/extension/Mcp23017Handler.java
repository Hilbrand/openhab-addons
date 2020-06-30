package org.openhab.binding.gpio.internal.extension;

import static com.pi4j.gpio.extension.mcp.MCP23017Pin.*;
import static org.openhab.binding.gpio.internal.GPIOBindingConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class Mcp23017Handler implements GPIOExtensionHandler<I2CConfiguration> {

    public static final String ID = "mcp23017";

    private static final Map<String, Pin> PIN_MAP = new HashMap<>();

    static {
        PIN_MAP.put(CHANNEL_A0, GPIO_A0);
        PIN_MAP.put(CHANNEL_A1, GPIO_A1);
        PIN_MAP.put(CHANNEL_A2, GPIO_A2);
        PIN_MAP.put(CHANNEL_A3, GPIO_A3);
        PIN_MAP.put(CHANNEL_A4, GPIO_A4);
        PIN_MAP.put(CHANNEL_A5, GPIO_A5);
        PIN_MAP.put(CHANNEL_A6, GPIO_A6);
        PIN_MAP.put(CHANNEL_A7, GPIO_A7);
        PIN_MAP.put(CHANNEL_B0, GPIO_B0);
        PIN_MAP.put(CHANNEL_B1, GPIO_B1);
        PIN_MAP.put(CHANNEL_B2, GPIO_B2);
        PIN_MAP.put(CHANNEL_B3, GPIO_B3);
        PIN_MAP.put(CHANNEL_B4, GPIO_B4);
        PIN_MAP.put(CHANNEL_B5, GPIO_B5);
        PIN_MAP.put(CHANNEL_B6, GPIO_B6);
        PIN_MAP.put(CHANNEL_B7, GPIO_B7);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GpioController gpioController;

    public Mcp23017Handler(final GpioController gpioController) {
        this.gpioController = gpioController;
    }

    @Override
    public GpioProvider createGpioProvider(final I2CConfiguration configuration)
            throws UnsupportedBusNumberException, IOException {
        final MCP23017GpioProvider mcp = new MCP23017GpioProvider(configuration.getBusNumber(),
                configuration.getAddressHex());
        mcp.setPollingTime(configuration.getPollingInterval());
        return mcp;
    }

    public void addChannel(final ChannelUID channelUID) {
        final String id = channelUID.getId();
        final String pinKey = id.substring(id.length() - 3, id.length());
        final Pin pin = PIN_MAP.get(pinKey);

    }

    // create channels

    // refresh channel

}
