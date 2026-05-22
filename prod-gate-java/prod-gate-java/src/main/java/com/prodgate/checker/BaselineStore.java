package com.prodgate.checker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe store for API baseline response times.
 *
 * JSON structure on disk:
 * {
 *   "get_users": {
 *     "baseline_ms": 210,
 *     "last_ms":     220,
 *     "last_updated": "2024-03-15T14:00:00Z"
 *   },
 *   ...
 * }
 *
 * Rules:
 *  - First time an API is seen → its current time becomes the baseline.
 *  - Subsequent runs          → baseline is NEVER auto-updated (only via CLI tool).
 *  - last_ms is always updated so you can see the trend.
 */
public class BaselineStore {

    private final Path         path;
    private final ObjectMapper mapper;
    private final ObjectNode   root;

    public BaselineStore(Path baselinePath) throws IOException {
        this.path   = baselinePath;
        this.mapper = new ObjectMapper();

        if (Files.exists(baselinePath)) {
            JsonNode node = mapper.readTree(baselinePath.toFile());
            this.root = node instanceof ObjectNode on ? on : mapper.createObjectNode();
        } else {
            this.root = mapper.createObjectNode();
        }
    }

    /**
     * Returns the baseline for this API, or -1 if no baseline exists yet.
     */
    public synchronized long getBaseline(String apiName) {
        JsonNode entry = root.get(apiName);
        if (entry == null || !entry.has("baseline_ms")) return -1L;
        return entry.get("baseline_ms").asLong(-1L);
    }

    /**
     * Called after each measurement.
     * Sets baseline on first run; always updates last_ms.
     */
    public synchronized void record(String apiName, long measuredMs) {
        ObjectNode entry = root.has(apiName)
            ? (ObjectNode) root.get(apiName)
            : mapper.createObjectNode();

        // First time → set baseline
        if (!entry.has("baseline_ms")) {
            entry.put("baseline_ms", measuredMs);
        }

        entry.put("last_ms",      measuredMs);
        entry.put("last_updated", Instant.now().toString());
        root.set(apiName, entry);
    }

    /**
     * Flush the updated JSON to disk.
     */
    public synchronized void save() throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
    }

    /**
     * Expose all entries for reporting.
     */
    public Map<String, long[]> allEntries() {
        Map<String, long[]> out = new HashMap<>();
        root.fields().forEachRemaining(e -> {
            JsonNode v = e.getValue();
            long baseline = v.has("baseline_ms") ? v.get("baseline_ms").asLong(-1) : -1;
            long last     = v.has("last_ms")      ? v.get("last_ms").asLong(-1)      : -1;
            out.put(e.getKey(), new long[]{baseline, last});
        });
        return out;
    }
}
