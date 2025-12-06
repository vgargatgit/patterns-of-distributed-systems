package sim.store;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Simulated network link with random latency; used even in single-node runs
 * to keep the abstraction consistent for later milestones.
 */
public final class Link {
    private final String id;
    private final Duration minLatency;
    private final Duration maxLatency;
    private final Random random;
    private final EventLog log;
    private final double failureProbability;

    public Link(String id, Duration minLatency, Duration maxLatency, Random random, EventLog log) {
        this(id, minLatency, maxLatency, random, log, 0.0);
    }

    public Link(String id, Duration minLatency, Duration maxLatency, Random random, EventLog log, double failureProbability) {
        if (maxLatency.compareTo(minLatency) < 0) {
            throw new IllegalArgumentException("maxLatency must be >= minLatency");
        }
        if (failureProbability < 0 || failureProbability > 1) {
            throw new IllegalArgumentException("failureProbability must be between 0 and 1");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.minLatency = Objects.requireNonNull(minLatency, "minLatency");
        this.maxLatency = Objects.requireNonNull(maxLatency, "maxLatency");
        this.random = Objects.requireNonNull(random, "random");
        this.log = Objects.requireNonNull(log, "log");
        this.failureProbability = failureProbability;
    }

    public void deliver(String description, Runnable action) {
        deliver(description, () -> {
            action.run();
            return null;
        }, null);
    }

    public <T> T deliver(String description, Supplier<T> action) {
        return deliver(description, action, null);
    }

    public <T> T deliver(String description, Supplier<T> action, Map<String, String> metadata) {
        long jitterMillis = jitterMillis();
        log.info(id, "deliver", MapBuilder.of("desc", description, "delayMs", Long.toString(jitterMillis)), metadata);
        sleepQuietly(jitterMillis);
        maybeFail(description, metadata);
        return action.get();
    }

    private long jitterMillis() {
        long min = minLatency.toMillis();
        long max = maxLatency.toMillis();
        if (max == min) {
            return min;
        }
        return min + Math.abs(random.nextLong()) % (max - min + 1);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void maybeFail(String description, Map<String, String> metadata) {
        if (random.nextDouble() <= failureProbability) {
            log.info(id, "deliver-failed", MapBuilder.of("desc", description, "reason", "link-drop"), metadata);
            throw new LinkFailureException("Link " + id + " dropped " + description);
        }
    }
}
