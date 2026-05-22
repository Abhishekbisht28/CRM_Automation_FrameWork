package com.prodgate.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodgate.config.GateConfig;
import com.prodgate.model.GateResult;
import com.prodgate.reporter.ConsoleReporter;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Phase 2 — API performance checker.
 *
 * For each endpoint in apis.json:
 *   1. Send HTTP request and measure wall-clock response time.
 *   2. Compare against hard limit (default 600 ms).
 *   3. Compare against stored baseline (regression guard).
 *   4. Record result and update baseline store.
 *
 * Gate is blocked when:
 *   - slowApiCount >= config.maxSlowApis   (hard-limit violations)
 *   - any API regresses beyond threshold   (regression violation)
 */
public class ApiPerformanceChecker {

    private final GateConfig     config;
    private final ConsoleReporter console;
    private final ObjectMapper   mapper = new ObjectMapper();

    public ApiPerformanceChecker(GateConfig config, ConsoleReporter console) {
        this.config  = config;
        this.console = console;
    }

    public void check(GateResult result) {
        console.section("PHASE 2 — API Performance & Regression Check");

        if (!Files.exists(config.getApisPath())) {
            console.warn("No apis.json found at " + config.getApisPath() + " — skipping API checks");
            return;
        }

        // ── Load API list ─────────────────────────────────────
        List<Map<String, Object>> apis;
        try {
            apis = mapper.readValue(config.getApisPath().toFile(),
                    new TypeReference<>() {});
        } catch (IOException e) {
            console.warn("Could not parse apis.json: " + e.getMessage());
            return;
        }

        // ── Load baseline store ───────────────────────────────
        BaselineStore baseline;
        try {
            baseline = new BaselineStore(config.getBaselinePath());
        } catch (IOException e) {
            console.warn("Could not load baseline: " + e.getMessage() + " — starting fresh");
            try { baseline = new BaselineStore(config.getBaselinePath()); }
            catch (IOException ex) { console.warn("Baseline unavailable."); return; }
        }

        console.info(String.format("Checking %d API endpoint(s)  |  Hard limit: %dms  |  Max slow: %d  |  Regression threshold: %d%%",
                apis.size(), config.getApiHardLimitMs(), config.getMaxSlowApis(), config.getRegressionPct()));

        // ── HTTP client with timeout ──────────────────────────
        RequestConfig reqConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.of(10, TimeUnit.SECONDS))
            .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
            .build();

        try (CloseableHttpClient http = HttpClients.custom()
                .setDefaultRequestConfig(reqConfig).build()) {

            int slowCount = 0;

            for (Map<String, Object> apiDef : apis) {
                String name   = str(apiDef, "name",   "api_" + apiDef.hashCode());
                String method = str(apiDef, "method", "GET").toUpperCase();
                String url    = str(apiDef, "url",    "");

                if (url.isBlank()) {
                    console.warn("Skipping entry with no URL: " + name);
                    continue;
                }

                // ── Measure ───────────────────────────────────
                long   storedBaseline = baseline.getBaseline(name);
                long[] measurement    = measure(http, method, url, apiDef);
                int    httpStatus     = (int) measurement[0];
                long   responseMs     = measurement[1];

                baseline.record(name, responseMs);

                // ── Evaluate ──────────────────────────────────
                boolean overHardLimit = responseMs > config.getApiHardLimitMs();
                boolean regression    = false;
                int     regressPct    = 0;

                if (storedBaseline > 0 && responseMs > storedBaseline) {
                    regressPct = (int)(((responseMs - storedBaseline) * 100) / storedBaseline);
                    regression = regressPct >= config.getRegressionPct();
                }

                String failReason = null;
                if (overHardLimit && regression) {
                    failReason = String.format("exceeds hard limit (%dms > %dms) AND regression vs baseline (+%d%%)",
                        responseMs, config.getApiHardLimitMs(), regressPct);
                } else if (overHardLimit) {
                    failReason = String.format("exceeds hard limit (%dms > %dms)",
                        responseMs, config.getApiHardLimitMs());
                } else if (regression) {
                    failReason = String.format("regression vs baseline (%dms > %dms baseline, +%d%%)",
                        responseMs, storedBaseline, regressPct);
                }

                // Store result (using package-visible factory)
                var apiResult = GateResult.newApiResult(name, method, url, httpStatus,
                        responseMs, storedBaseline, failReason);
                result.addApiResult(apiResult);

                if (overHardLimit) {
                    slowCount++;
                    result.addSlowApi(apiResult);
                }
                if (regression) {
                    result.addRegressionApi(apiResult);
                }

                // ── Console output ────────────────────────────
                String baselineLabel = storedBaseline > 0
                    ? "  [baseline: " + storedBaseline + "ms]" : "  [first run — baseline set]";
                String icon = failReason != null ? "🔴" : "✅";

                if (failReason != null) {
                    console.fail(String.format("%s  %-30s  [%-4s]  %4dms  HTTP%d%s",
                        icon, name, method, responseMs, httpStatus, baselineLabel));
                    console.fail("    ↳ " + failReason);
                } else {
                    console.ok(String.format("%s  %-30s  [%-4s]  %4dms  HTTP%d%s",
                        icon, name, method, responseMs, httpStatus, baselineLabel));
                }
            }

            // ── Summary ───────────────────────────────────────
            console.separator();
            console.info("Slow APIs (>" + config.getApiHardLimitMs() + "ms): "
                + slowCount + " / allowed: " + config.getMaxSlowApis());
            console.info("Regression APIs: " + result.getRegressionApis().size());

            if (slowCount >= config.getMaxSlowApis()) {
                console.fail("Too many slow APIs (" + slowCount + " ≥ " + config.getMaxSlowApis() + ") — GATE BLOCKED");
            }
            if (!result.getRegressionApis().isEmpty()) {
                console.fail("Performance regression(s) detected — GATE BLOCKED");
            }

        } catch (IOException e) {
            console.warn("HTTP client error: " + e.getMessage());
        }

        // ── Persist baseline ──────────────────────────────────
        try {
            baseline.save();
            console.info("Baseline saved to: " + config.getBaselinePath());
        } catch (IOException e) {
            console.warn("Could not save baseline: " + e.getMessage());
        }
    }

    // ── HTTP measurement ──────────────────────────────────────

    /**
     * Returns [httpStatusCode, elapsedMs].
     * On error returns [0, 10000].
     */
    @SuppressWarnings("unchecked")
    private long[] measure(CloseableHttpClient http, String method,
                           String url, Map<String, Object> apiDef) {
        ClassicHttpRequest request = buildRequest(method, url, apiDef);
        long start = System.currentTimeMillis();
        try {
            try (ClassicHttpResponse response = http.executeOpen(null, request, null)) {
                long elapsed = System.currentTimeMillis() - start;
                return new long[]{ response.getCode(), elapsed };
            }
        } catch (Exception e) {
            return new long[]{ 0, 10_000L };
        }
    }

    @SuppressWarnings("unchecked")
    private ClassicHttpRequest buildRequest(String method, String url,
                                             Map<String, Object> apiDef) {
        ClassicHttpRequest req = switch (method) {
            case "POST"   -> new HttpPost(url);
            case "PUT"    -> new HttpPut(url);
            case "PATCH"  -> new HttpPatch(url);
            case "DELETE" -> new HttpDelete(url);
            default       -> new HttpGet(url);
        };

        // Headers
        Object headersObj = apiDef.get("headers");
        if (headersObj instanceof Map<?, ?> headers) {
            headers.forEach((k, v) -> req.addHeader(String.valueOf(k), String.valueOf(v)));
        }

        // Body
        String body = str(apiDef, "body", "");
        if (!body.isBlank() && req instanceof HttpPost post) {
            post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        }

        return req;
    }

    private static String str(Map<String, Object> map, String key, String fallback) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : fallback;
    }
}
