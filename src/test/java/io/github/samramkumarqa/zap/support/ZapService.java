package io.github.samramkumarqa.zap.support;

import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ApiResponseList;
import org.zaproxy.clientapi.core.ApiResponseSet;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A thin, test-friendly wrapper over the OWASP ZAP daemon's REST API.
 *
 * <p>The ZAP daemon itself is started outside the suite (Docker in CI, the desktop app or {@code zap.sh -daemon}
 * locally) and the suite talks to it over HTTP. Nothing here attacks anything on its own: the spider and active
 * scanner only ever run against {@code target.url}, which must be an app you are authorised to test.
 */
public class ZapService {

    private final ClientApi api;
    private final String apiKey;
    private final Duration pollInterval = Duration.ofSeconds(2);

    public ZapService(String host, int port, String apiKey) {
        // A blank key means ZAP was started with the API key disabled (fine for a local throwaway daemon)
        this.apiKey = (apiKey == null || apiKey.isBlank()) ? null : apiKey;
        this.api = new ClientApi(host, port, this.apiKey);
    }

    public void waitUntilReady(int timeoutSeconds) {
        try {
            api.waitForSuccessfulConnectionToZap(timeoutSeconds);
        } catch (ClientApiException e) {
            throw new IllegalStateException(
                    "Could not reach the ZAP daemon. Start ZAP first (see the README) and check zap.host/zap.port.", e);
        }
    }

    /** Waits for the passive scanner to drain the queue the functional tests filled while browsing. */
    public void waitForPassiveScan(Duration timeout) {
        pollUntil(timeout, "passive scan", () -> {
            int remaining = Integer.parseInt(value(api.pscan.recordsToScan()));
            return remaining == 0 ? 100 : -1;
        });
    }

    /** Crawls the target to discover pages and endpoints the browser didn't reach. Returns when it hits 100%. */
    public void spider(String targetUrl, Duration timeout) {
        try {
            String scanId = value(api.spider.scan(targetUrl, null, "true", null, null));
            pollUntil(timeout, "spider", () -> Integer.parseInt(value(api.spider.status(scanId))));
        } catch (ClientApiException e) {
            throw new ZapException("spider failed", e);
        }
    }

    /** Runs the active scanner against the target. This sends crafted (attack) requests, so only to an in-scope app. */
    public void activeScan(String targetUrl, Duration timeout) {
        try {
            String scanId = value(api.ascan.scan(targetUrl, "true", "false", null, null, null));
            pollUntil(timeout, "active scan", () -> Integer.parseInt(value(api.ascan.status(scanId))));
        } catch (ClientApiException e) {
            throw new ZapException("active scan failed", e);
        }
    }

    public List<Alert> alerts(String baseUrl) {
        try {
            ApiResponse response = api.alert.alerts(baseUrl, "0", null, null);
            List<Alert> alerts = new ArrayList<>();
            for (ApiResponse item : ((ApiResponseList) response).getItems()) {
                ApiResponseSet set = (ApiResponseSet) item;
                alerts.add(new Alert(
                        set.getStringValue("alert"),
                        Risk.fromLabel(set.getStringValue("risk")),
                        set.getStringValue("confidence"),
                        set.getStringValue("url"),
                        set.getStringValue("param")));
            }
            return alerts;
        } catch (ClientApiException e) {
            throw new ZapException("could not read alerts", e);
        }
    }

    /** Writes ZAP's own HTML report via the stable OTHER endpoint, independent of the report add-on's version. */
    public void writeHtmlReport(Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            byte[] html = api.callApiOther("core", "other", "htmlreport", Map.of());
            Files.write(destination, html);
        } catch (ClientApiException | IOException e) {
            throw new ZapException("could not write the HTML report", e);
        }
    }

    private interface Progress {
        int percent() throws ClientApiException;
    }

    private void pollUntil(Duration timeout, String what, Progress progress) {
        Instant deadline = Instant.now().plus(timeout);
        try {
            int percent;
            while ((percent = progress.percent()) < 100) {
                if (Instant.now().isAfter(deadline)) {
                    throw new ZapException(what + " did not finish within " + timeout.toSeconds() + "s (reached "
                            + Math.max(percent, 0) + "%)", null);
                }
                Thread.sleep(pollInterval.toMillis());
            }
        } catch (ClientApiException e) {
            throw new ZapException(what + " status check failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ZapException(what + " was interrupted", e);
        }
    }

    private static String value(ApiResponse response) {
        return ((ApiResponseElement) response).getValue();
    }

    /** Unchecked wrapper so step code isn't littered with checked ZAP exceptions. */
    public static class ZapException extends RuntimeException {
        ZapException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
