/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.stookwijzer.internal.handler;

import static org.openhab.binding.stookwijzer.internal.StookwijzerBindingConstants.*;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.stookwijzer.internal.api.StookwijzerProperties;
import org.openhab.binding.stookwijzer.internal.api.StookwijzerWmsClient;
import org.openhab.binding.stookwijzer.internal.config.StookwijzerConfiguration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link StookwijzerHandler} handles a Stookwijzer thing.
 *
 * It periodically polls the RIVM WMS service and updates all channels
 * with the latest stookwijzer data for the configured RD New location.
 *
 * Channel mapping:
 * <ul>
 * <li>pc4 → StringType — 4-digit postal code area</li>
 * <li>model_runtime → StringType — when the model was last run</li>
 * <li>lki → DecimalType — air quality index 1–11 (or -1 if unavailable)</li>
 * <li>wind → DecimalType — wind speed in m/s</li>
 * <li>wind_bft → DecimalType — wind speed in Beaufort</li>
 * <li>windrichting → DecimalType — wind direction in degrees (-1 if unavailable)</li>
 * <li>advies_0..18 → DecimalType — advice code: -1=geen, 0=geel, 1=oranje, 2=rood</li>
 * <li>definitief_0..18 → OnOffType — whether the advice for that block is final</li>
 * </ul>
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class StookwijzerHandler extends BaseThingHandler {

    private static final String[] ALL_CHANNELS = { CHANNEL_PC4, CHANNEL_MODEL_RUNTIME, CHANNEL_LKI, CHANNEL_WIND,
            CHANNEL_WIND_BFT, CHANNEL_WINDRICHTING, CHANNEL_ADVIES_0, CHANNEL_ADVIES_6, CHANNEL_ADVIES_12,
            CHANNEL_ADVIES_18, CHANNEL_DEFINITIEF_0, CHANNEL_DEFINITIEF_6, CHANNEL_DEFINITIEF_12,
            CHANNEL_DEFINITIEF_18 };

    private final Logger logger = LoggerFactory.getLogger(StookwijzerHandler.class);

    private @Nullable ScheduledFuture<?> pollingJob;
    private final StookwijzerWmsClient wmsClient;
    private StookwijzerConfiguration config = new StookwijzerConfiguration();

    public StookwijzerHandler(Thing thing) {
        super(thing);
        wmsClient = new StookwijzerWmsClient();
    }

    @Override
    public void initialize() {
        config = getConfigAs(StookwijzerConfiguration.class);

        logger.debug("Initialising Stookwijzer handler for RD ({}, {}), refresh={}min", config.rdX, config.rdY,
                config.refreshInterval);

        updateStatus(ThingStatus.UNKNOWN);

        // First poll immediately, then at the configured interval
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollAndUpdate, 0, config.refreshInterval, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        final ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            pollAndUpdate();
        }
        // All channels are read-only; no write commands are processed
    }

    // -------------------------------------------------------------------------
    // Polling
    // -------------------------------------------------------------------------

    private void pollAndUpdate() {
        final Optional<StookwijzerProperties> result = wmsClient.fetchData(config.rdX, config.rdY);

        if (result.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Geen data ontvangen van de RIVM WMS service. Controleer de RD-coördinaten en internetverbinding.");
            setAllChannelsUndef();
            return;
        }

        updateStatus(ThingStatus.ONLINE);
        updateChannels(result.get());
    }

    // -------------------------------------------------------------------------
    // Channel updates
    // -------------------------------------------------------------------------

    private void updateChannels(StookwijzerProperties p) {
        // Identification & timing
        updateStringChannel(CHANNEL_PC4, p.pc4);
        updateStringChannel(CHANNEL_MODEL_RUNTIME, p.model_runtime);

        // Meteorological data
        updateDecimalChannel(CHANNEL_LKI, p.lki);
        updateDecimalChannel(CHANNEL_WIND, p.wind);
        updateDecimalChannel(CHANNEL_WIND_BFT, p.wind_bft);
        updateDecimalChannel(CHANNEL_WINDRICHTING, p.windrichting);

        // Advice per 6-hour time block
        updateStringChannel(CHANNEL_ADVIES_0, String.valueOf(p.advies_0));
        updateStringChannel(CHANNEL_ADVIES_6, String.valueOf(p.advies_6));
        updateStringChannel(CHANNEL_ADVIES_12, String.valueOf(p.advies_12));
        updateStringChannel(CHANNEL_ADVIES_18, String.valueOf(p.advies_18));

        // Whether each advice is final
        updateOnOffChannel(CHANNEL_DEFINITIEF_0, p.definitief_0);
        updateOnOffChannel(CHANNEL_DEFINITIEF_6, p.definitief_6);
        updateOnOffChannel(CHANNEL_DEFINITIEF_12, p.definitief_12);
        updateOnOffChannel(CHANNEL_DEFINITIEF_18, p.definitief_18);
    }

    private void setAllChannelsUndef() {
        for (String ch : ALL_CHANNELS) {
            updateState(ch, UnDefType.UNDEF);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void updateStringChannel(String channelId, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            updateState(channelId, new StringType(value));
        } else {
            updateState(channelId, UnDefType.UNDEF);
        }
    }

    private void updateDecimalChannel(String channelId, Number value) {
        updateState(channelId, new DecimalType(value.doubleValue()));
    }

    private void updateOnOffChannel(String channelId, boolean value) {
        updateState(channelId, value ? OnOffType.ON : OnOffType.OFF);
    }
}
