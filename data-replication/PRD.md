# Simulated Distributed Key-Value Store — PRD

## Purpose
- Build a learning-focused, simulation-only distributed key-value store to illustrate common failure modes and solutions.
- Emphasize clarity over performance; prefer demonstrable behaviors (logs, metrics, visualizations) to production-grade throughput.

## Goals & Non-Goals
- Goals: highlight replication/consensus trade-offs, surface failure scenarios, offer pluggable policies (delays, drops, crashes), and iterate incrementally from single-node to clustered behavior.
- Non-Goals: production durability guarantees, security/auth, multi-tenant concerns, and cross-language clients.

## Success Criteria
- Each learning module shows a reproducible failure and a follow-up mitigation.
- Configurable simulations (random seeds, latency ranges, crash frequency) produce deterministic reruns when seeded.
- Clear observability: structured logs and small stats (e.g., op counts, latency buckets) per node and per link.

## Users & Use Cases
- Primary: learners exploring distributed systems by running scenarios locally.
- Use cases: stepwise labs (single node → replication), debugging exercises (e.g., replay WAL), and toggling network conditions to see system behavior.

## System Overview
- Node abstraction holds key-value data, persistent log (after WAL added), and replicated state once clustering is introduced.
- Link abstraction simulates asynchronous, lossy, delayed delivery between nodes; supports configurable delay distributions and drop/duplicate toggles.
- Client simulator issues `put(key, value)` and `get(key)` requests with optional concurrency and retries.
- Coordinator role (leader) emerges in later stages to serialize writes and drive replication; followers apply logs.

## Functional Requirements (phased)
1) **Single node baseline**
   - In-memory key-value map; supports `put`, `get`.
   - Optional random crash that loses memory state; instrumentation to show crash timing.
2) **Durability via Write-Ahead Log**
   - Append-only WAL persisted locally; on crash, node replays log to restore state.
   - Include log corruption/fault injection toggles to demonstrate recovery limits.
3) **Abstract network links**
   - Links between nodes with random latency, drops, and optional reordering/duplication.
   - Seeded randomness for reproducible runs; per-link stats.
4) **Leader-follower replication**
   - Leader handles writes; followers replicate from leader log/stream.
   - Detection of stale followers; simple catch-up mechanism.
   - Client read policy toggle: `read-from-leader` vs `read-from-any` with staleness warning.
5) **Failure scenarios & mitigations**
   - Leader crash: election or manual promotion; demonstrate write unavailability window.
   - Network partitions: highlight divergent states; reconciliation strategy (e.g., majority commit marker).
   - Slow follower: backpressure or snapshotting to catch up.
6) **Observability & tooling**
   - Structured event log for operations and failures; human-readable timeline view.
   - CLI controls to start/stop nodes, trigger crashes, set delays, and inspect state.
   - Optional visualization (text-based) showing cluster topology and message flow.

## Non-Functional Requirements
- Deterministic mode via seeds; must be able to replay the same run.
- Lightweight: runnable with standard language/runtime, no external services.
- Tests per phase demonstrating the failure and the fix.

## Architecture & Components
- `Node`: state machine for KV operations, WAL module, replication hooks.
- `Log`: append/replay API; supports truncation/compaction later.
- `Link`: simulated transport with delay/drop model.
- `Cluster`: manages nodes, leader assignment, replication, and client routing policy.
- `Client`: issues commands, can simulate concurrent writers/readers.

## Milestones & Deliverables
- M0: Single-node KV with crash simulation and observability hooks.
- M1: WAL added; crash/restart restores state; tests showing before/after. Prevents data loss on node crashes observed in logs (e.g., `event=crash` → `state=lost`, subsequent GET returning `null`).
- M2: Network/link simulator; multiple nodes exchanging messages with delays/drops.
- M3: Leader/follower replication with basic consistency guarantees and read-policy toggles.
- M4: Failure drills (partition, leader crash, slow follower) with visible effects and mitigations.
- M5: Optional polish: snapshot/compaction, visualization improvements, and richer stats.

## Observed M0 Failures (motivation for WAL)
- Crash wipes in-memory state, GETs can return `null` after restart:
  ```
  [..] actor=node-1 event=crash context=put
  [..] actor=node-1 event=crashed reason=put state=lost
  [..] actor=client-1 event=op-timeout desc=client-put attempt=1 id=0 reason=node-crash
  [..] actor=node-1 event=restart state=empty
  [..] actor=node-1 event=get key=k5 value=null   <-- lost data after crash
  ```
- Repeated crashes force retries and eventually exhaust attempts:
  ```
  [..] actor=node-1 event=crash context=get
  [..] actor=client-1 event=op-timeout desc=client-get attempt=3 id=4 reason=node-crash
  [..] actor=client-1 event=op-failed desc=client-get id=4 reason=Node node-1 crashed during get
  ```
- Link drops can also exhaust retries, leaving writes incomplete:
  ```
  [..] actor=loopback event=deliver-failed desc=client-put reason=link-drop {id: 7, K: k4, V: v4}
  [..] actor=client-1 event=op-failed desc=client-put id=7 reason=Link loopback dropped client-put
  ```
WAL (M1) directly addresses crash-induced data loss; link drops remain but combine with WAL to allow safe replay/retry without losing acknowledged writes.

## Open Questions
- Election approach: manual promotion vs lightweight timeout-based election?
- Commit rule: majority-based commit markers vs leader-driven acks only?
- Snapshotting: needed early or deferred until after initial learning objectives?

## References
- Patterns of Distributed Systems — Umesh Joshi.
