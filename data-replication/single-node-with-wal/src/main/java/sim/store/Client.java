package sim.store;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple client that routes requests to a single node over a simulated link.
 */
public final class Client {
    private final String id;
    private final Node node;
    private final Link link;
    private final EventLog log;
    private final int maxRetries;
    private final Runnable onNodeCrash;
    private final AtomicInteger requestSeq = new AtomicInteger();

    public Client(String id, Node node, Link link, EventLog log) {
        this(id, node, link, log, 3, () -> {});
    }

    public Client(String id, Node node, Link link, EventLog log, int maxRetries, Runnable onNodeCrash) {
        this.id = Objects.requireNonNull(id, "id");
        this.node = Objects.requireNonNull(node, "node");
        this.link = Objects.requireNonNull(link, "link");
        this.log = Objects.requireNonNull(log, "log");
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be >= 1");
        }
        this.maxRetries = maxRetries;
        this.onNodeCrash = Objects.requireNonNull(onNodeCrash, "onNodeCrash");
    }

    public void put(String key, String value) {
        int reqId = requestSeq.getAndIncrement();
        runWithRetry("client-put", () -> {
            node.put(key, value);
            log.info(id, "put-ok", MapBuilder.of("key", key, "value", value, "id", Integer.toString(reqId)));
            return null;
        }, requestMetadata(reqId, key, value));
    }

    public Optional<String> get(String key) {
        int reqId = requestSeq.getAndIncrement();
        return runWithRetry("client-get", () -> {
            var value = node.get(key);
            log.info(id, "get-ok", MapBuilder.of("key", key, "value", value.orElse("null"), "id", Integer.toString(reqId)));
            return value;
        }, requestMetadata(reqId, key, "?"));
    }

    private <T> T runWithRetry(String desc, SupplierWithException<T> action, Map<String, String> metadata) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return link.deliver(desc, () -> {
                    try {
                        return action.apply();
                    } catch (RuntimeException e) {
                        // Let runtime exceptions propagate (including crashes).
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, metadata);
            } catch (LinkFailureException e) {
                lastFailure = e;
                log.info(id, "link-retry", MapBuilder.of("desc", desc, "attempt", Integer.toString(attempt), "id", metadata.get("id")));
            } catch (NodeCrashedException e) {
                lastFailure = e;
                log.info(id, "op-timeout", MapBuilder.of("desc", desc, "attempt", Integer.toString(attempt), "id", metadata.get("id"), "reason", "node-crash"));
                onNodeCrash.run();
            }
        }
        if (lastFailure != null) {
            log.info(id, "op-failed", MapBuilder.of("desc", desc, "id", metadata.get("id"), "reason", lastFailure.getMessage()));
            throw lastFailure;
        }
        throw new LinkFailureException("Retries exhausted for " + desc);
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T apply() throws Exception;
    }

    private Map<String, String> requestMetadata(int id, String key, String value) {
        return MapBuilder.of("id", Integer.toString(id), "K", key, "V", value);
    }
}
