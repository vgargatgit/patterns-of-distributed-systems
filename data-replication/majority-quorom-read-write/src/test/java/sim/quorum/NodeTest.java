package sim.quorum;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NodeTest {
    private EventLog log() {
        return new EventLog(new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    void putAndGet() {
        Node node = new Node("node-1", log());
        node.put("k", "v");
        assertTrue(node.get("k").isPresent());
        assertEquals("v", node.get("k").orElseThrow());
    }
}
