package sim.quorum;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientTest {
    private EventLog log() {
        return new EventLog(new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    void clientUsesRandomEntryNodeAndStillReadsWrittenValue() {
        EventLog log = log();
        int n = 3;
        Cluster cluster = Cluster.demo(n, 0.0, new Random(2), log);
        Link[] clientLinks = new Link[n];
        for (int i = 0; i < n; i++) {
            clientLinks[i] = new Link("client-" + i, Duration.ZERO, Duration.ZERO, 0.0, new Random(3 + i), log);
        }
        Client client = new Client("client-1", cluster, clientLinks, new Random(4), log);

        client.put("k", "v");
        assertEquals("v", client.get("k").orElseThrow());
    }
}
