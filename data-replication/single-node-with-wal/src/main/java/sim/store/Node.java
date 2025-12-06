package sim.store;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node in-memory key-value store with crash injection and observability.
 */
public final class Node {
    private final String id;
    private final EventLog log;
    private final CrashInjector crashInjector;
    private final WriteAheadLog wal;
    private final Map<String, String> data = new ConcurrentHashMap<>();
    private volatile boolean alive = true;

    public Node(String id, EventLog log, CrashInjector crashInjector, WriteAheadLog wal) {
        this.id = Objects.requireNonNull(id, "id");
        this.log = Objects.requireNonNull(log, "log");
        this.crashInjector = Objects.requireNonNull(crashInjector, "crashInjector");
        this.wal = Objects.requireNonNull(wal, "wal");
        restoreFromWal();
    }

    public void put(String key, String value) {
        ensureAlive();
        wal.appendPut(key, value);
        injectCrash("put");
        data.put(key, value);
        log.info(id, "put", MapBuilder.of("key", key, "value", value));
    }

    public Optional<String> get(String key) {
        ensureAlive();
        injectCrash("get");
        String value = data.get(key);
        log.info(id, "get", MapBuilder.of("key", key, "value", value));
        return Optional.ofNullable(value);
    }

    public void crashAndLoseState(String reason) {
        alive = false;
        data.clear();
        log.info(id, "crashed", MapBuilder.of("reason", reason, "state", "lost"));
    }

    public void restart() {
        alive = true;
        restoreFromWal();
        log.info(id, "restart", MapBuilder.of("state", "from-wal", "entries", Integer.toString(data.size())));
    }

    private void ensureAlive() {
        if (!alive) {
            throw new NodeCrashedException("Node " + id + " is down");
        }
    }

    private void injectCrash(String context) {
        try {
            crashInjector.maybeCrash(context);
        } catch (NodeCrashedException e) {
            crashAndLoseState(context);
            throw e;
        }
    }

    private void restoreFromWal() {
        Map<String, String> restored = wal.replay();
        data.clear();
        data.putAll(restored);
    }
}
