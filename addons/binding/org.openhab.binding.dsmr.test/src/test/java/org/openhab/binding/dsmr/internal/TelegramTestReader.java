package org.openhab.binding.dsmr.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;

public class TelegramTestReader {
    private static final String TELEGRAM_EXT = ".telegram";

    public static byte[] readTelegram(String name) throws IOException, URISyntaxException {
        try (InputStream is = TelegramTestReader.class.getResourceAsStream(name + TELEGRAM_EXT)) {
            return IOUtils.toByteArray(is);
        }
    }
}
