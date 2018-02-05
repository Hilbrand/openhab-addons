package org.openhab.binding.dsmr.internal.p1telegram;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.openhab.binding.dsmr.internal.TelegramTestReader;
import org.openhab.binding.dsmr.internal.device.cosem.CosemObject;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramListener;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramListener.TelegramState;
import org.openhab.binding.dsmr.internal.device.p1telegram.P1TelegramParser;

/**
 * Test class for {@link P1TelegramParser}.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@RunWith(value = Parameterized.class)
public class P1TelegramParserTest {

    // @formatter:off
    @Parameters(name = "{0}")
    public static final List<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "Iskra_AM550", 35, },
            { "Iskra_MT382", 35, },
            { "Landis_Gyr_ZCF110", 23, },
            { "Sagemcom_XS210", 30, },

        });
    }
    // @formatter:on

    @Parameter(0)
    public String telegramName;

    @Parameter(1)
    public int numberOfCosemObjects;

    @Test
    public void testParsing() throws IOException, URISyntaxException {
        final List<CosemObject> cosemObjects = new ArrayList<>();
        AtomicReference<TelegramState> telegramState = new AtomicReference<>();
        P1TelegramListener telegramListener = (co, ts) -> {
            cosemObjects.addAll(co);
            telegramState.set(ts);
        };
        P1TelegramParser parser = new P1TelegramParser(false, telegramListener);
        byte[] telegram = TelegramTestReader.readTelegram(telegramName);
        parser.parseData(telegram, 0, telegram.length);
        assertEquals("Expected State should be OK", TelegramState.OK, telegramState.get());
        assertEquals("Expected number of objects", numberOfCosemObjects, cosemObjects.size());
    }
}
