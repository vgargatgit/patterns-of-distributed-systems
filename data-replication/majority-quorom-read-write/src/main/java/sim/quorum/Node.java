package sim.quorum;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory node; no WAL here to keep focus on quorum logic.
 */
final class Node {
    private final String id;
    private final Map<String, String> data = new ConcurrentHashMap<>();
    private final EventLog log;

    Node(String id, EventLog log) {
        this.id = Objects.requireNonNull(id);
        this.log = Objects.requireNonNull(log);
    }

    void put(String key, String value) {
        data.put(key, value);
        log.info(id, "put", MapBuilder.of("key", key, "value", value));
    }

    Optional<String> get(String key) {
        String value = data.get(key);
        log.info(id, "get", MapBuilder.of("key", key, "value", value));
        return Optional.ofNullable(value);
    }
}
