package sim.quorum;

import java.util.Random;

/**
 * Demonstrates majority quorum writes and reads with link drops.
 */
public final class Simulation {
    public static void main(String[] args) {
        int nodeCount = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        double dropProbability = args.length > 1 ? Double.parseDouble(args[1]) : 0.2;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 42L;

        EventLog log = new EventLog(System.out);
        Random random = new Random(seed);
        Cluster cluster = Cluster.demo(nodeCount, dropProbability, random, log);
        Link[] clientLinks = new Link[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            clientLinks[i] = new Link("client-link-node-" + i, java.time.Duration.ofMillis(5), java.time.Duration.ofMillis(40), dropProbability, random, log);
        }
        Client client = new Client("client-1", cluster, clientLinks, random, log);

        for (int i = 0; i < 5; i++) {
            String key = "k" + i;
            String value = "v" + i;
            try {
                client.put(key, value);
                client.get(key);
            } catch (QuorumException | LinkFailureException e) {
                log.info("simulation", "iteration-failed", MapBuilder.of("i", Integer.toString(i), "reason", e.getMessage()));
            }
        }

        log.info("simulation", "finished", MapBuilder.of("nodes", Integer.toString(nodeCount), "dropProb", Double.toString(dropProbability)));
    }
}
