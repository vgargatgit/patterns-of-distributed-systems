package sim.quorum;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Simulated link with latency and drop probability.
 */
final class Link {
    private final String id;
    private final Duration minLatency;
    private final Duration maxLatency;
    private final double dropProbability;
    private final Random random;
    private final EventLog log;

    Link(String id, Duration minLatency, Duration maxLatency, double dropProbability, Random random, EventLog log) {
        if (dropProbability < 0 || dropProbability > 1) {
            throw new IllegalArgumentException("dropProbability must be 0..1");
        }
        this.id = Objects.requireNonNull(id);
        this.minLatency = Objects.requireNonNull(minLatency);
        this.maxLatency = Objects.requireNonNull(maxLatency);
        this.dropProbability = dropProbability;
        this.random = Objects.requireNonNull(random);
        this.log = Objects.requireNonNull(log);
    }

    <T> T deliver(String desc, Supplier<T> action, Map<String, String> meta) {
        long delay = jitterMillis();
        log.info(id, "deliver", MapBuilder.of("desc", desc, "delayMs", Long.toString(delay)), meta);
        sleep(delay);
        if (random.nextDouble() <= dropProbability) {
            log.info(id, "deliver-failed", MapBuilder.of("desc", desc, "reason", "link-drop"), meta);
            throw new LinkFailureException("Link " + id + " dropped " + desc);
        }
        return action.get();
    }

    private long jitterMillis() {
        long min = minLatency.toMillis();
        long max = maxLatency.toMillis();
        if (max == min) return min;
        return min + Math.abs(random.nextLong()) % (max - min + 1);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
