package sim.quorum;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

final class Client {
    private final Cluster cluster;
    private final EventLog log;
    private final String id;
    private final Link[] clientLinks;
    private final Random random;

    Client(String id, Cluster cluster, Link[] clientLinks, Random random, EventLog log) {
        this.id = Objects.requireNonNull(id);
        this.cluster = Objects.requireNonNull(cluster);
        this.clientLinks = Objects.requireNonNull(clientLinks);
        this.random = Objects.requireNonNull(random);
        this.log = Objects.requireNonNull(log);
    }

    void put(String key, String value) {
        int entry = chooseNode();
        Link link = clientLinks[entry];
        var meta = MapBuilder.of("entry", Integer.toString(entry), "key", key, "value", value);
        try {
            cluster.put(key, value, entry, link, meta);
            log.info(id, "put-ok", MapBuilder.of("key", key, "value", value, "entry", Integer.toString(entry)));
        } catch (RuntimeException e) {
            log.info(id, "put-failed", MapBuilder.of("key", key, "value", value, "reason", e.getMessage(), "entry", Integer.toString(entry)));
            throw e;
        }
    }

    Optional<String> get(String key) {
        int entry = chooseNode();
        Link link = clientLinks[entry];
        var meta = MapBuilder.of("entry", Integer.toString(entry), "key", key);
        try {
            Optional<String> value = cluster.get(key, entry, link, meta);
            log.info(id, "get-ok", MapBuilder.of("key", key, "value", value.orElse("null"), "entry", Integer.toString(entry)));
            return value;
        } catch (RuntimeException e) {
            log.info(id, "get-failed", MapBuilder.of("key", key, "reason", e.getMessage(), "entry", Integer.toString(entry)));
            throw e;
        }
    }

    private int chooseNode() {
        return random.nextInt(clientLinks.length);
    }
}
