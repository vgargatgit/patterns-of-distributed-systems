package sim.quorum;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClusterTest {
    private EventLog log() {
        return new EventLog(new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    void writeAndReadQuorumSucceedsWhenMajorityAvailable() {
        EventLog log = log();
        int n = 3;
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodes.add(new Node("node-" + i, log));
        }
        Link[][] mesh = new Link[n][n];
        Random random = new Random(1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                mesh[i][j] = new Link("link-" + i + "-" + j, Duration.ZERO, Duration.ZERO, 0.0, random, log);
            }
        }
        Cluster cluster = new Cluster(nodes, mesh, log);
        Link clientLink = new Link("client-0", Duration.ZERO, Duration.ZERO, 0.0, random, log);

        cluster.put("k", "v", 0, clientLink, MapBuilder.of("key", "k", "value", "v"));
        assertEquals("v", cluster.get("k", 1, clientLink, MapBuilder.of("key", "k")).orElseThrow());
    }

    @Test
    void writeQuorumFailsWhenDropsExceedMajority() {
        EventLog log = log();
        int n = 3;
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodes.add(new Node("node-" + i, log));
        }
        Link[][] mesh = new Link[n][n];
        Random random = new Random(1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // entry node 0 drops to others; self link always succeeds
                double drop = (i == 0 && j != 0) ? 1.0 : 0.0;
                mesh[i][j] = new Link("link-" + i + "-" + j, Duration.ZERO, Duration.ZERO, drop, random, log);
            }
        }
        Cluster cluster = new Cluster(nodes, mesh, log);
        Link clientLink = new Link("client-0", Duration.ZERO, Duration.ZERO, 0.0, random, log);

        assertThrows(QuorumException.class, () ->
                cluster.put("k", "v", 0, clientLink, MapBuilder.of("key", "k", "value", "v")));
    }
}
