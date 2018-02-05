package org.openhab.binding.dsmr.internal.discovery;

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
import org.openhab.binding.dsmr.internal.meter.DSMRMeterDescriptor;

@RunWith(value = Parameterized.class)
public class DSMRMeterDetectorTest {

    // @formatter:off
    @Parameters(name = "{0}")
    public static final List<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "Iskra_AM550", 3, },
            { "Iskra_MT382", 3, },
            { "Landis_Gyr_ZCF110", 3, },
            { "Sagemcom_XS210", 2, },
        });
    }
    // @formatter:on

    @Parameter(0)
    public String telegramName;

    @Parameter(1)
    public int numberOfMeters;

    @Test
    public void testDetectMeters() throws IOException, URISyntaxException {
        final List<CosemObject> cosemObjects = new ArrayList<>();
        AtomicReference<TelegramState> telegramState = new AtomicReference<>();
        P1TelegramListener telegramListener = (co, ts) -> {
            cosemObjects.addAll(co);
            telegramState.set(ts);
        };
        P1TelegramParser parser = new P1TelegramParser(false, telegramListener);
        byte[] telegram = TelegramTestReader.readTelegram(telegramName);
        parser.parseData(telegram, 0, telegram.length);

        DSMRMeterDetector detector = new DSMRMeterDetector();
        List<DSMRMeterDescriptor> detectMeters = detector.detectMeters(cosemObjects);
        assertEquals("Should detect correct number of meters", numberOfMeters, detectMeters.size());
    }

}
