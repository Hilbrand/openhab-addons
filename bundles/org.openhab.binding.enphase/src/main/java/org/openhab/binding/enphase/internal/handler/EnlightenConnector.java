package org.openhab.binding.enphase.internal.handler;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.enphase.internal.EnvoyConfiguration;
import org.openhab.binding.enphase.internal.EnvoyConnectionException;
import org.openhab.binding.enphase.internal.dto.EnlightenSystemsDTO;
import org.openhab.binding.enphase.internal.dto.EnvoyErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Methods to make API calls to the Enlighten cloud service.
 *
 * @author Joe Inkenbrandt - Initial contribution
 */
@NonNullByDefault
public class EnlightenConnector {

    private static final String API_BASE_URL = "https://api.enphaseenergy.com/api/v2";
    private static final String SYSTEM_URL = "/systems";
    private static final String INVENTORY_URL = "/systems/{systemid}/inventory";

    private final Logger logger = LoggerFactory.getLogger(EnlightenConnector.class);
    private final Gson gson = new GsonBuilder().create();
    private final HttpClient httpClient;

    private static final long CONNECT_TIMEOUT_SECONDS = 5;

    private String key = "";
    private String userId = "";
    private String systemId = "";

    public EnlightenConnector(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Sets the Envoy connection configuration.
     *
     * @param configuration the configuration to set
     */
    public void setConfiguration(final EnvoyConfiguration configuration) {
        this.key = "4679f0e480f706300b86b7315de7944a";
        this.userId = "4d6a4d334e444d334f413d3d0a";
        this.systemId = "2339377";
    }

    private String processUrl(String url) {
        return url.replace("{systemid}", this.systemId);
    }

    public EnlightenSystemsDTO getSystems() throws EnvoyConnectionException {
        return retrieveData(SYSTEM_URL, this::getSystemsJson);
    }

    private @Nullable EnlightenSystemsDTO getSystemsJson(final String json) {
        return gson.fromJson(json, EnlightenSystemsDTO.class);
    }

    private synchronized <T> T retrieveData(final String urlPath, final Function<String, @Nullable T> jsonConverter)
            throws EnvoyConnectionException {

        try {
            final URI uri = URI
                    .create(API_BASE_URL + processUrl(urlPath) + "?key=" + this.key + "&user_id=" + this.userId);
            logger.trace("Retrieving data from '{}'", uri);
            final Request request = httpClient.newRequest(uri).method(HttpMethod.GET).timeout(CONNECT_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            final ContentResponse response = request.send();
            final String content = response.getContentAsString();

            logger.trace("Envoy returned data for '{}' with status {}: {}", urlPath, response.getStatus(), content);
            if (response.getStatus() == HttpStatus.OK_200) {
                final T result = jsonConverter.apply(content);
                if (result == null) {
                    throw new EnvoyConnectionException("No data received");
                }
                return result;
            } else {
                final @Nullable EnvoyErrorDTO error = gson.fromJson(content, EnvoyErrorDTO.class);

                logger.info("Envoy returned an error: {}", error);
                throw new EnvoyConnectionException(error == null ? response.getReason() : error.info);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnvoyConnectionException("Interrupted");
        } catch (final TimeoutException e) {
            logger.debug("TimeoutException: {}", e.getMessage());
            throw new EnvoyConnectionException("Connection timeout: ", e);
        } catch (final ExecutionException e) {
            logger.debug("ExecutionException: {}", e.getMessage(), e);
            throw new EnvoyConnectionException("Could not retrieve data: ", e.getCause());
        }

    }

}
