package sim.store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal append-only write-ahead log for key/value puts.
 */
public final class WriteAheadLog {
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final String nodeId;
    private final Path file;
    private final EventLog log;

    public WriteAheadLog(String nodeId, Path file, EventLog log) {
        this.nodeId = nodeId;
        this.file = file;
        this.log = log;
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to initialize WAL at " + file, e);
        }
    }

    public synchronized void appendPut(String key, String value) {
        String line = "PUT " + encode(key) + " " + encode(value) + "\n";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(line);
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to append to WAL " + file, e);
        }
        log.info(nodeId, "wal-append", MapBuilder.of("key", key, "value", value));
    }

    public synchronized Map<String, String> replay() {
        Map<String, String> data = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return data;
        }
        int lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(" ", 3);
                if (parts.length != 3 || !"PUT".equals(parts[0])) {
                    log.info(nodeId, "wal-skip", MapBuilder.of("line", Integer.toString(lines), "reason", "bad-format"));
                    continue;
                }
                String key = decode(parts[1]);
                String value = decode(parts[2]);
                data.put(key, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to replay WAL " + file, e);
        }
        log.info(nodeId, "wal-replay", MapBuilder.of("entries", Integer.toString(data.size()), "lines", Integer.toString(lines)));
        return data;
    }

    private String encode(String value) {
        return ENC.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String encoded) {
        return new String(DEC.decode(encoded), StandardCharsets.UTF_8);
    }
}
