package sim.quorum;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Cluster orchestrates quorum reads/writes across nodes.
 */
final class Cluster {
    private final List<Node> nodes;
    private final Link[][] mesh; // client-to-node link is provided externally; mesh is node-to-node
    private final EventLog log;
    private final int quorumSize;

    Cluster(List<Node> nodes, Link[][] mesh, EventLog log) {
        if (mesh.length != nodes.size()) {
            throw new IllegalArgumentException("mesh size must match nodes");
        }
        this.nodes = List.copyOf(nodes);
        this.mesh = mesh;
        this.log = Objects.requireNonNull(log);
        this.quorumSize = nodes.size() / 2 + 1;
    }

    void put(String key, String value, int entryNode, Link clientLink, Map<String, String> meta) {
        clientLink.deliver("client-put", () -> {
            replicatePut(key, value, entryNode, meta);
            return null;
        }, meta);
    }

    private void replicatePut(String key, String value, int entryNode, Map<String, String> meta) {
        int successes = 0;
        List<String> failures = new ArrayList<>();
        for (int target = 0; target < nodes.size(); target++) {
            int nodeIdx = target;
            Link link = mesh[entryNode][target];
            if (link == null) continue;
            try {
                link.deliver("put", () -> {
                    nodes.get(nodeIdx).put(key, value);
                    return null;
                }, MapBuilder.of("from", nodeId(entryNode), "to", nodeId(nodeIdx), "key", key, "value", value));
                successes++;
            } catch (LinkFailureException e) {
                failures.add(nodeId(nodeIdx));
            }
        }
        if (successes < quorumSize) {
            throw new QuorumException("Write quorum failed (success=" + successes + ", need=" + quorumSize + ", failures=" + failures + ")");
        }
        log.info("cluster", "write-commit", MapBuilder.of("key", key, "value", value, "acks", Integer.toString(successes)));
    }

    Optional<String> get(String key, int entryNode, Link clientLink, Map<String, String> meta) {
        return clientLink.deliver("client-get", () -> replicateGet(key, entryNode), meta);
    }

    private Optional<String> replicateGet(String key, int entryNode) {
        Map<String, Integer> votes = new HashMap<>();
        int successes = 0;
        for (int target = 0; target < nodes.size(); target++) {
            int nodeIdx = target;
            Link link = mesh[entryNode][target];
            if (link == null) continue;
            try {
                Optional<String> value = link.deliver("get", () -> nodes.get(nodeIdx).get(key), MapBuilder.of("from", nodeId(entryNode), "to", nodeId(nodeIdx), "key", key));
                String val = value.orElse(null);
                votes.merge(val, 1, Integer::sum);
                successes++;
            } catch (LinkFailureException e) {
                // skip failed node
            }
        }
        var majority = majorityValue(votes, quorumSize);
        if (majority.isPresent() && successes >= quorumSize) {
            String decided = majority.get();
            log.info("cluster", "read-commit", MapBuilder.of("key", key, "value", decided, "acks", Integer.toString(successes)));
            return Optional.ofNullable(decided);
        }
        throw new QuorumException("Read quorum failed (acks=" + successes + ", need=" + quorumSize + ", votes=" + votes + ")");
    }

    static Cluster demo(int nodeCount, double dropProbability, Random random, EventLog log) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(new Node(nodeId(i), log));
        }
        Link[][] mesh = new Link[nodeCount][nodeCount];
        for (int from = 0; from < nodeCount; from++) {
            for (int to = 0; to < nodeCount; to++) {
                mesh[from][to] = new Link("link-" + nodeId(from) + "-" + nodeId(to), Duration.ofMillis(5), Duration.ofMillis(40), dropProbability, random, log);
            }
        }
        return new Cluster(nodes, mesh, log);
    }

    private Optional<String> majorityValue(Map<String, Integer> votes, int quorum) {
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            if (entry.getValue() >= quorum) {
                return Optional.ofNullable(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private static String nodeId(int i) {
        return "node-" + i;
    }
}
