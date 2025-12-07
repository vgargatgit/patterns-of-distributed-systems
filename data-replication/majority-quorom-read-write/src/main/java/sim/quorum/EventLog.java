package sim.quorum;

import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.StringJoiner;

final class EventLog {
    private final PrintStream out;

    EventLog(PrintStream out) {
        this.out = out;
    }

    void info(String actor, String event, Map<String, String> details) {
        info(actor, event, details, null);
    }

    void info(String actor, String event, Map<String, String> details, Map<String, String> meta) {
        var ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        var joiner = new StringJoiner(" ");
        if (details != null) {
            details.forEach((k, v) -> joiner.add(k + "=" + v));
        }
        String suffix = joiner.toString();
        String metaStr = formatMeta(meta);
        if (!metaStr.isEmpty()) {
            suffix = (suffix + " " + metaStr).trim();
        }
        out.printf("[%s] actor=%s event=%s %s%n", ts, actor, event, suffix);
    }

    private String formatMeta(Map<String, String> meta) {
        if (meta == null || meta.isEmpty()) {
            return "";
        }
        var joiner = new StringJoiner(", ");
        meta.forEach((k, v) -> joiner.add(k + ": " + v));
        return "{" + joiner + "}";
    }
}
