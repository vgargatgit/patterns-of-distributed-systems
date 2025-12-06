package sim.store;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;

/**
 * Small driver to demonstrate M1: single node with WAL, crashable, observable.
 */
public final class Simulation {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        double crashProbability = args.length > 1 ? Double.parseDouble(args[1]) : 0.25;
        double linkFailureProbability = args.length > 2 ? Double.parseDouble(args[2]) : 0.1;

        EventLog log = new EventLog(System.out);
        Random random = new Random(seed);

        CrashInjector crashInjector = new CrashInjector("node-1", log, random, crashProbability);
        WriteAheadLog wal = new WriteAheadLog("node-1", Path.of("wal/node-1.log"), log);
        Node node = new Node("node-1", log, crashInjector, wal);
        Link loopback = new Link("loopback", Duration.ofMillis(5), Duration.ofMillis(50), random, log, linkFailureProbability);
        Client client = new Client("client-1", node, loopback, log, 3, node::restart);

        for (int i = 0; i < 8; i++) {
            String key = "k" + i;
            String value = "v" + i;
            try {
                client.put(key, value);
                client.get(key);
            } catch (LinkFailureException e) {
                log.info("simulation", "request-failed", MapBuilder.of("iteration", Integer.toString(i), "reason", "link-failure"));
            } catch (NodeCrashedException e) {
                log.info("simulation", "node-down", MapBuilder.of("iteration", Integer.toString(i), "reason", e.getMessage()));
                node.restart();
            }
        }

        log.info("simulation", "finished", MapBuilder.of("seed", Long.toString(seed), "crashProbability", Double.toString(crashProbability)));
    }
}
