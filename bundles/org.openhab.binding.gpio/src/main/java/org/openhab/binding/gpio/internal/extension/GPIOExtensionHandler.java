package org.openhab.binding.gpio.internal.extension;

import java.io.IOException;

import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public interface GPIOExtensionHandler<T> {

    GpioProvider createGpioProvider(T configuration) throws UnsupportedBusNumberException, IOException;

}
