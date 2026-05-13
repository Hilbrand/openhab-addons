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
package org.openhab.binding.stookwijzer.internal.api;

import static org.openhab.binding.stookwijzer.internal.StookwijzerBindingConstants.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * {@link StookwijzerWmsClient} fetches stookwijzer data from the RIVM WMS service.
 *
 * The WMS GetFeatureInfo endpoint is called with:
 * - A 1×1 metre BBOX built from the configured RD New (EPSG:28992) coordinates: x,y,x+1,y+1
 * - A 2×2 pixel image size so the single point lands in the centre
 * - info_format=application/json
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class StookwijzerWmsClient {

    private static final String URL_TEMPLATE = WMS_BASE_URL
            + "?service=WMS&REQUEST=GetFeatureInfo&QUERY_LAYERS=stookwijzer_v2&LAYERS=stookwijzer_v2"
            + "&info_format=application/json&feature_count=1&I=1&J=1&WIDTH=1&HEIGHT=1&CRS=EPSG:28992&BBOX=%d,%d,%d,%d";

    private static final int HTTP_TIMEOUT_SECONDS = 15;

    private final Logger logger = LoggerFactory.getLogger(StookwijzerWmsClient.class);

    private final HttpClient httpClient;
    private final Gson gson;

    public StookwijzerWmsClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS)).build();
        this.gson = new Gson();
    }

    /**
     * Fetch Stookwijzer data for the given RD New coordinates.
     *
     * @param rdX X-coordinate in RD New (EPSG:28992)
     * @param rdY Y-coordinate in RD New (EPSG:28992)
     * @return Optional containing the properties, or empty if the request failed
     *         or no feature was returned
     */
    public Optional<StookwijzerProperties> fetchData(final int rdX, final int rdY) {
        final String url = String.format(URL_TEMPLATE, rdX, rdY, rdX + 1, rdY + 1);
        logger.debug("Fetching Stookwijzer data from: {}", url);

        try {
            final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS)).header("Accept", "application/json").GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("WMS request returned HTTP {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            return parseResponse(response.body());

        } catch (IOException e) {
            logger.warn("IO error fetching Stookwijzer data: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while fetching Stookwijzer data");
            return Optional.empty();
        }
    }

    /**
     * Parse the GeoJSON FeatureCollection response and extract the first feature's properties.
     */
    private Optional<StookwijzerProperties> parseResponse(final String json) {
        try {
            final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            final JsonArray features = root.getAsJsonArray("features");

            if (features == null || features.size() == 0) {
                logger.debug("No features in WMS response — location may be outside NL coverage");
                return Optional.empty();
            }

            final JsonElement firstFeature = features.get(0);
            if (firstFeature == null || !firstFeature.isJsonObject()) {
                return Optional.empty();
            }

            final JsonObject props = firstFeature.getAsJsonObject().getAsJsonObject("properties");
            if (props == null) {
                logger.warn("Feature has no properties in WMS response");
                return Optional.empty();
            }

            final StookwijzerProperties properties = gson.fromJson(props, StookwijzerProperties.class);
            logger.debug("Parsed Stookwijzer properties: pc4={}, advies_0={}, lki={}, wind={}", properties.pc4,
                    properties.advies_0, properties.lki, properties.wind);

            return Optional.of(properties);

        } catch (Exception e) {
            logger.warn("Failed to parse WMS response: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
