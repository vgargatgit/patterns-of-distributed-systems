# M1: Single-Node KV Simulation with WAL (Java 21)

## Run (javac)
```
javac -source 21 -target 21 -d out $(find src/main/java -name "*.java")
java -cp out sim.store.Simulation
```

## Run (Maven)
```
mvn clean package
java -jar target/single-node-with-wal-0.1.0-SNAPSHOT.jar
```
With all args: `java -jar target/single-node-with-wal-0.1.0-SNAPSHOT.jar 42 0.25 0.1`

## Notes
- `Simulation` accepts optional args: `seed` (long), `crashProbability` (double 0-1), `linkFailureProbability` (double 0-1).
- Logs show client calls, link delay, node operations, and crashes that wipe in-memory state.
- WAL file: `wal/node-1.log` (Base64-url encoded key/value per line). Delete it to start fresh; keep it to see recovery after crashes or restarts.

## Log format (M1)
- General shape: `[ISO-8601-ts] actor=<component> event=<name> key=<k> value=<v> ...`
- Deliver events append a payload in braces when present, e.g., `{id: 0, K: k1, V: v1}` to show request id and key/value sent over the link.
- Actors:
  - `loopback`: simulated link from client to node (shows latency per request).
  - `node-1`: the in-memory node handling operations.
  - `client-1`: caller confirming successful operations.
  - `simulation`: driver reporting iterations or crash/restart handling.
- Common events:
  - `deliver`: link scheduled a request; includes `delayMs` and `desc`.
  - `deliver-failed`: link dropped a request before reaching the node.
  - `link-retry`: client will retry the request because the link dropped it.
  - `op-timeout`: client treated a node crash as a timeout and will retry; request id stays the same.
  - `op-failed`: retries exhausted; request failed and bubbled up.
  - `put` / `get`: node handled the request; includes key/value.
  - `put-ok` / `get-ok`: client observed success; echoes key/value (null when missing).
  - `wal-append`: entry persisted to the WAL before applying to memory.
  - `wal-replay`: node reconstructed in-memory state from the WAL on startup/restart (shows counts).
  - `wal-skip`: malformed WAL line was ignored.
  - `crash`: crash injection triggered inside an operation; throws `NodeCrashedException`.
  - `crashed`: node recorded it went down and wiped state.
  - `restart`: node came back up and restored from WAL (`state=from-wal`).

Tip: you can add your own markers (e.g., prefix events with `FAIL=`) when we start logging explicit failures; for now logs stay ASCII to keep output portable.

### Example interaction (line-by-line)
```
[2025-12-06T16:49:52.282759722Z] actor=loopback event=deliver delayMs=30 desc=client-put {id: 0, K: k0, V: v0}
[2025-12-06T16:49:52.325321200Z] actor=node-1 event=put value=v0 key=k0
[2025-12-06T16:49:52.326880034Z] actor=client-1 event=put-ok value=v0 key=k0
[2025-12-06T16:49:52.329215332Z] actor=loopback event=deliver delayMs=32 desc=client-get {id: 1, K: k0, V: ?}
[2025-12-06T16:49:52.361996693Z] actor=node-1 event=get value=v0 key=k0
[2025-12-06T16:49:52.363657725Z] actor=client-1 event=get-ok value=v0 key=k0
```
- `loopback deliver (client-put)`: the simulated link schedules the client’s PUT request, noting the randomized latency (`delayMs=30`) and the payload `{id: 0, K: k0, V: v0}`. The `id` stays stable across retries of the same request.
- `node-1 put`: after the delay, the node processes the PUT, storing `k0 -> v0`.
- `client-1 put-ok`: the client is notified the PUT succeeded.
- `loopback deliver (client-get)`: the link schedules the subsequent GET with its own delay (`delayMs=32`) and payload showing the request id and key.
- `node-1 get`: the node reads the value for `k0`, returning `v0`.
- `client-1 get-ok`: the client receives the value and logs success.

Note on flow: the link is the first actor because every client call is sent over a simulated network hop. The link logs when it schedules delivery, then (after the delay) the node handles the request and the client logs the observed result.

Crash/retry note: if the node crashes mid-request, the client logs `op-timeout` with the same `id`, calls `restart` (in the default simulation wiring), and retries until max retries are exhausted.
If the link drops too many times or the node stays down across retries, you will see `op-failed` and the simulation will log `request-failed` for that iteration.

WAL note: puts are logged before being applied; after a crash/restart, the node replays `wal/node-1.log` so successful (logged) puts survive memory loss.
