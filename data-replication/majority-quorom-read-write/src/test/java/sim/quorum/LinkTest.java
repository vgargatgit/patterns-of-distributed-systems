package sim.quorum;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LinkTest {
    private EventLog log() {
        return new EventLog(new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    void deliver_runsActionWhenNotDropped() {
        Link link = new Link("link", Duration.ZERO, Duration.ZERO, 0.0, new Random(1), log());
        AtomicBoolean ran = new AtomicBoolean(false);
        String result = link.deliver("desc", () -> {
            ran.set(true);
            return "ok";
        }, MapBuilder.of("meta", "m"));
        assertEquals("ok", result);
        assertEquals(true, ran.get());
    }

    @Test
    void deliver_throwsOnDrop() {
        Link link = new Link("link", Duration.ZERO, Duration.ZERO, 1.0, new Random(1), log());
        assertThrows(LinkFailureException.class, () -> link.deliver("desc", () -> null, MapBuilder.of("meta", "m")));
    }
}
