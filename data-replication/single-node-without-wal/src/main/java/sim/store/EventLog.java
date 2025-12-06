package sim.store;

import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Minimal structured logging helper to keep simulation output readable.
 */
public final class EventLog {
    private final PrintStream out;

    public EventLog(PrintStream out) {
        this.out = out;
    }

    public void info(String actor, String event, Map<String, String> details) {
        info(actor, event, details, null);
    }

    public void info(String actor, String event, Map<String, String> details, Map<String, String> metadata) {
        var timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        var joiner = new StringJoiner(" ");
        if (details != null) {
            details.forEach((k, v) -> joiner.add(k + "=" + v));
        }
        String meta = formatMetadata(metadata);
        String suffix = meta.isEmpty() ? joiner.toString() : (joiner + " " + meta).trim();
        out.printf("[%s] actor=%s event=%s %s%n", timestamp, actor, event, suffix);
    }

    private String formatMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        var joiner = new StringJoiner(", ");
        metadata.forEach((k, v) -> joiner.add(k + ": " + v));
        return "{" + joiner + "}";
    }
}
