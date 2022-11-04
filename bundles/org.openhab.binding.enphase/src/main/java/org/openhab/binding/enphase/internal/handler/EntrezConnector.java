package org.openhab.binding.enphase.internal.handler;

import java.net.HttpCookie;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openhab.binding.enphase.internal.EnvoyConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntrezConnector {

    private static final String LOGIN_URL = "https://entrez.enphaseenergy.com/login";
    private static final String TOKEN_URL = "https://entrez.enphaseenergy.com/entrez_tokens";

    private final Logger logger = LoggerFactory.getLogger(EntrezConnector.class);
    private final HttpClient httpClient;

    private static final long CONNECT_TIMEOUT_SECONDS = 5;

    public EntrezConnector(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getJwt(String username, String password, String siteId, String serialNum)
            throws EnvoyConnectionException {

        String session = login(username, password);

        Fields fields = new Fields();
        fields.put("Site", siteId);
        fields.put("serialNum", serialNum);

        try {
            final URI uri = URI.create(TOKEN_URL);
            logger.trace("Retrieving data from '{}'", uri);
            final Request request = httpClient.newRequest(uri).method(HttpMethod.POST)
                    .cookie(new HttpCookie("SESSION", session)).content(new FormContentProvider(fields))
                    .timeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            final ContentResponse response = request.send();

            Document document = Jsoup.parse(response.getContentAsString());
            Elements elements = document.select("#JWTToken");

            return elements.first().text();

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

    private String login(String username, String password) throws EnvoyConnectionException {

        Fields fields = new Fields();
        fields.put("username", username);
        fields.put("password", password);

        try {
            final URI uri = URI.create(LOGIN_URL);
            logger.trace("Retrieving data from '{}'", uri);
            final Request request = httpClient.newRequest(uri).method(HttpMethod.POST)
                    .content(new FormContentProvider(fields)).timeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            final ContentResponse response = request.send();

            if (response.getStatus() == 200 && response.getHeaders().containsKey("Set-Cookie")) {
                String cookies[] = response.getHeaders().get("Set-Cookie").split(";");

                for (String s : cookies) {
                    if (s.startsWith("SESSION=")) {
                        return s.replaceAll("SESSION=", "");
                    }
                }
            }

            throw new EnvoyConnectionException("Could not login to Entrez JWT Portal");

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
