# Majority Quorum Read/Write (Java 21)

Implements a small simulation of majority-quorum writes and reads across N nodes with simulated link drops and latency.

## Run (Maven)
```
cd majority-quorom-read-write
mvn clean package
java -jar target/majority-quorom-read-write-0.1.0-SNAPSHOT.jar
```
Args: `java -jar target/majority-quorom-read-write-0.1.0-SNAPSHOT.jar <nodeCount=3> <dropProb=0.2> <seed=42>`

## Topology
- Client has one link to every node (chooses a random entry node per request).
- Nodes have a full mesh of links to every other node; the entry node fans out reads/writes to all.
- No leader: any node can act as entry; quorum math is used to decide success.

## Quorum semantics
- Write quorum: needs `⌊N/2⌋ + 1` successful writes (via entry node fan-out). Logs `write-commit` or `put-failed`.
- Read quorum: entry node gathers values from all nodes, succeeds if some value has ≥ quorum votes; else `get-failed`.
- Link drops: simulated on client→node and node→node links; if enough nodes respond, quorum still succeeds.

## Log highlights
- `deliver` / `deliver-failed`: link behavior per node per request.
- `put`, `get`: node-side operations.
- `write-commit`, `read-commit`: cluster reached quorum.
- `put-failed`, `get-failed`: quorum not met.
- `iteration-failed`: simulation loop caught an error.

## Why majority helps
- With 3 nodes, 2 acks are required. A single node drop or link failure often still allows progress.
- Reads require a majority agreement on the value, reducing stale reads—but split-brain scenarios can still surface inconsistencies (to be addressed with a leader in the next milestone).
