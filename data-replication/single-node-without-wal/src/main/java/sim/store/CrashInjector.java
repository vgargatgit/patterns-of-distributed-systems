package sim.store;

import java.util.Objects;
import java.util.Random;

import static sim.store.MapBuilder.of;

/**
 * Injects probabilistic crashes to surface durability gaps.
 */
public final class CrashInjector {
    private final String nodeId;
    private final EventLog log;
    private final Random random;
    private final double crashProbability;

    public CrashInjector(String nodeId, EventLog log, Random random, double crashProbability) {
        if (crashProbability < 0 || crashProbability > 1) {
            throw new IllegalArgumentException("crashProbability must be between 0 and 1");
        }
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.log = Objects.requireNonNull(log, "log");
        this.random = Objects.requireNonNull(random, "random");
        this.crashProbability = crashProbability;
    }

    public void maybeCrash(String context) {
        if (random.nextDouble() <= crashProbability) {
            log.info(nodeId, "crash", of("context", context));
            throw new NodeCrashedException("Node " + nodeId + " crashed during " + context);
        }
    }
}
