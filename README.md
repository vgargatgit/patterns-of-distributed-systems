# Database Transactions vs Distributed Systems Consistency  
_A translation guide between two worlds_

When you read about databases and distributed systems, you’re often looking at the **same conceptual problems** described with **different vocabularies**.

This note is a “dictionary” that helps map:

- **Database world:** transactions, isolation levels, ACID
- **Distributed systems world:** replicas, consistency models, CAP, quorums

---

## 1. Core Objects: What Are We Manipulating?

| Concept                           | Database Transaction World                                  | Distributed Systems World                                    |
|----------------------------------|-------------------------------------------------------------|--------------------------------------------------------------|
| Unit of operation                | **Transaction** (BEGIN…COMMIT)                             | **Request / Operation** (single read/write/RPC)              |
| Data being changed               | Rows, documents, keys                                      | **Replicated objects / partitions / shards**                 |
| System view                      | Usually **single logical node** (even if replicated underneath, it's hidden) | **Many nodes explicitly visible** (clients may hit different replicas) |

---

## 2. Correctness Guarantees: ACID vs CAP/Consistency

### Database vocabulary (ACID)

- **Atomicity**  
  All-or-nothing: the whole transaction commits or nothing does.

- **Consistency**  
  Transaction moves DB from one valid state to another (constraints, invariants).

- **Isolation**  
  Concurrent transactions should behave _as if_ they ran one by one (depending on isolation level).

- **Durability**  
  Once committed, data “sticks” even after crashes.

### Distributed systems vocabulary

- **Consistency (in CAP):**  
  Roughly: every read sees the latest **committed** write (this is very close to **linearizability**).

- **Availability:**  
  Every request gets a response (success or failure), even if some nodes are down.

- **Partition tolerance:**  
  System continues working despite network splits / message loss.

In practice, distributed systems talk more about:

- **Consistency models** (linearizable, sequential, causal, eventual, etc.)
- **Replication strategies** (primary–secondary, leaderless, quorums, etc.)

> Mapping hint:  
> - DB **Isolation** ↔ DS **Consistency model**  
> - DB **Durability** ↔ DS **Replication + logging + recovery**

---

## 3. Isolation Levels vs Consistency Models

### Database isolation levels

Common levels (SQL / traditional DB world):

- **Read Uncommitted**  
  May see uncommitted writes (dirty reads).

- **Read Committed**  
  Sees only committed data, but repeated reads may see different values (non-repeatable read).

- **Repeatable Read**  
  Same row read twice in a transaction returns the same value (but phantoms may appear).

- **Serializable**  
  Behavior is as if transactions ran one at a time, in some serial order.

These are about **interleavings of reads/writes inside a single database node’s transaction manager**.

### Distributed systems consistency models

Some common models:

- **Linearizability**  
  Every operation appears to take effect at a single point in time between call and response. If one client sees value `v`, no later read (in real time) can see an older value.

- **Sequential Consistency**  
  All nodes agree on some **global order of operations**, but that order doesn’t have to respect real-time boundaries.

- **Causal Consistency**  
  If operation B depends on A, everyone sees A before B. Unrelated operations can be seen in different orders.

- **Eventual Consistency**  
  If no new writes happen, all replicas eventually converge to the same value. During convergence, anything goes.

- **Session Guarantees** (per client):
  - Read-your-writes
  - Monotonic reads
  - Monotonic writes
  - Writes-follow-reads

> Mapping hint (approximate):  
> - **Serializable**  ≈  **Linearizable + Multi-object atomicity**  
> - **Read Committed / Repeatable Read**  ≈  various weaker consistency + some session guarantees  
> - **Eventual consistency**  is even weaker than typical DB isolation levels.

---

## 4. Anomalies: Different Words, Same Pain

### Database anomalies (isolation language)

- **Dirty read**  
  T1 reads uncommitted data from T2.

- **Non-repeatable read**  
  T1 reads row X twice, sees two different values because T2 updated X in between.

- **Phantom read**  
  T1 runs `SELECT ... WHERE condition`, T2 inserts a row matching that condition; T1 re-runs the query and sees extra row(s).

### Distributed systems anomalies (consistency language)

- **Stale read**  
  Read hits a replica that hasn’t seen latest write yet.

- **Read skew / Monotonicity violation**  
  One read sees new value, a later read (in real time) sees an older value (no **monotonic reads**).

- **Lost update (replication perspective)**  
  Two writes happen concurrently on different replicas; one overwrites the other without merging.

> Example mapping:

- DB **non-repeatable read** ↔ DS **stale read / non-monotonic read** across replicas.
- DB **read skew** ↔ DS seeing a mix of old and new values across multiple keys (no multi-key atomicity).

---

## 5. “Commit” vs “Quorum Success”

### Database “commit”

In a classic single-node RDBMS:

- **COMMIT** =  
  - The transaction log is flushed (fsync),
  - In-memory state is updated,
  - From now on, all transactions will see the committed changes (based on isolation level).

### Distributed “successful write”

In a replicated/quorum system:

- A write is “successful” when:
  - It’s been acknowledged by **w** replicas (in an n-replica system),
  - Or confirmed by the **leader + majority**, depending on the algorithm.

But **successful** here **does not guarantee** that all future reads will see it:

- If a read hits fewer replicas (or uses small r, or weak read policy), it might still see old data.
- That’s where your “one read sees new, later read sees old” scenario comes in.

> Mental model:  
> - DB COMMIT → after this, DB behaves consistent with its isolation level.  
> - DS “write acknowledged” → after this, reads _might_ see it depending on **consistency level** (ONE/QUORUM/ALL) and routing.

---

## 6. Session-Level Guarantees

Databases usually provide these implicitly once you’re inside a transaction (and often even across transactions, depending on config).

Distributed systems often **name them explicitly**:

- **Read-your-writes:**  
  After you write X, your subsequent reads will see that write.

- **Monotonic reads:**  
  If you’ve seen a value `v`, you’ll never later see a value **older** than `v`.

- **Monotonic writes:**  
  Your writes are applied in the order you issued them.

These are essentially **client-centric isolation guarantees** over replicated state.

> Mapping hint:  
> - A single DB connection with transactions often gives you something close to **read-your-writes + monotonic reads** by default.  
> - Distributed systems make you **choose** or configure these per client/operation.

---

## 7. Simplified “Rosetta Stone” Table

| Theme                    | Database World (Transactions)                  | Distributed Systems World (Replication/Consistency)             |
|-------------------------|-----------------------------------------------|------------------------------------------------------------------|
| “What are we controlling?” | Interleaving of txns on one logical DB       | Ordering & visibility across replicas and clients                |
| Strongest common guarantee | Serializable                                 | Linearizable (per key)                                          |
| Typical level used       | Read Committed / Repeatable Read             | “Read committed-ish” + **quorums** / **eventual consistency**   |
| Weird effect             | Non-repeatable read, phantom                 | Stale read, non-monotonic read, lost update                     |
| Client view              | Transaction boundaries                        | Session guarantees (read-your-writes, monotonic reads, etc.)    |
| Commit semantics         | Log flush + isolation rules applied          | Majority / quorum ack; visibility depends on read policy        |

---

## 8. How to Think When Reading Papers / Docs

When you see:

- **DB paper / docs** → think:
  - “What isolation level are they assuming?”
  - “What anomalies are allowed/not allowed inside a transaction?”

- **Distributed systems paper / NoSQL docs** → think:
  - “What consistency model are they giving me?”
  - “Do they promise linearizability, or just eventual consistency?”
  - “Do they give session guarantees (read-your-writes)?”

And mentally translate:

- **Isolation level** ⟶ “How much weirdness do they allow between concurrent operations?”
- **Consistency model** ⟶ “How much disagreement can replicas and clients have about ‘what happened when’?”

---



# Replication and Quorums — Notes

> ⚠️ This note is about *replication & consistency* in distributed databases.  
> For the “dictionary” between **database transaction isolation** and **distributed consistency models**, see:  
> **[Database Transactions vs Distributed Systems Consistency](./database-transactions-vs-distributed-systems-consistency.md)**  
> In particular, the “one read sees new, later read sees old” scenario here is a **violation of monotonic reads** in that vocabulary.

---

## 1. Why Replicate Data?

There are several reasons to replicate data across multiple nodes:

1. **Lower latency**  
   Keep data geographically close to users → reduce access latency.

2. **Higher availability & durability**  
   System can keep working even if some nodes fail, and data is less likely to be lost.

3. **Higher read throughput**  
   More replicas can serve more read requests in parallel.

Almost all distributed databases pick one of three high-level replication strategies:

- **Single-leader replication**
- **Multi-leader replication**
- **Leaderless replication**

(Here, we focus on single-leader and leaderless.)

---

## 2. Single-Leader Replication

Each node stores a copy of the database. The main task is to ensure that **changes on the leader** are **replicated to followers**.

### 2.1 Basic Idea

- One node is designated as the **leader** (or primary).
- All **writes** go to the leader:
  - Leader writes to its local storage.
  - Then sends the change (log/WAL entries) to **followers**.
- **Reads** can:
  - Either go only to the leader (stronger consistency), or
  - Go to any replica (better performance, but weaker guarantees).

---

## 3. Synchronous vs Asynchronous Replication

Consider a **3-node cluster** with 1 leader and 2 followers.

### 3.1 Mixed Sync/Async Example

- A write to the cluster is:
  - Replicated **synchronously** to one follower.
  - Replicated **asynchronously** to the other follower.
- The cluster returns **success** to the client **once the synchronous follower has acknowledged** the write.

So:

- **Leader + synchronous follower** are guaranteed to be up to date.
- **Asynchronous follower** may be slightly behind.

Not all followers can be synchronous: if one synchronous follower becomes slow or fails, it could **block writes**.  
In practice:

- Often **one follower is synchronous**, others are asynchronous.
- If the synchronous follower fails or lags too much, the system can promote an async follower to become the new synchronous one.

This “leader + 1 synchronous follower” setup is sometimes called **semi-synchronous replication**.

### 3.2 Quorum-Style Sync

Some systems go further: in an `n = 5` node cluster:

- A **majority** (e.g., 3 out of 5, including the leader) are updated **synchronously**.
- Remaining replicas are **asynchronous**.

This is essentially a **quorum write** (see [Quorums for Reading and Writing](#7-quorums-for-reading-and-writing)).

---

## 4. Handling Node Outages

Nodes can crash or be temporarily unreachable. The goal is:

- Keep the system running as a whole.
- Minimize the impact of any single node’s failure.

### 4.1 Follower Failure: Catch-Up Recovery

If a follower crashes or loses connectivity:

1. From its **write-ahead log (WAL)**, it knows the **last transaction it successfully applied**.
2. When it comes back, it:
   - Connects to the leader.
   - Requests all changes since that last applied log position.
   - Replays those changes to **catch up** with the leader.
3. Once caught up, it resumes normal streaming replication.

This is generally straightforward.

### 4.2 Leader Failure: Failover

Leader failure is more complex. The system must:

1. **Detect leader failure**  
   - Often via **heartbeats** and timeouts.
2. **Choose a new leader**  
   - Usually through an **election** (consensus) among replicas.
   - Best candidate: **most up-to-date follower**, to minimize data loss.
3. **Reconfigure the system**  
   - Clients must start sending **writes** to the new leader.
   - Other followers should start replicating **from the new leader**.
   - If the old leader comes back, it must:
     - Recognize that it has lost leadership.
     - Demote itself to follower and catch up from the new leader.

This is a **consensus problem**: all nodes must agree on **who the leader is**.

### 4.3 Data Loss and Up-To-Date Followers

To minimize data loss during failover:

- With **synchronous / semi-synchronous replication**, pick the **synchronous follower** as the new leader.
- With **asynchronous replication**, pick the follower with the **highest log sequence number (LSN)**.

Even then, some recent writes may be lost (e.g., last few milliseconds), but this might be acceptable in practice.

---

## 5. Leaderless Replication

Some systems abandon the idea of a single leader and allow **any replica** to accept **writes directly**.

- Historical examples: early replicated systems [1, 50].
- Modern “Dynamo-style” open-source systems:
  - **Riak**
  - **Cassandra**
  - **ScyllaDB**

These are often described as **leaderless** or **Dynamo-style replication**.

> ⚠️ Note: **Amazon DynamoDB** is not the same as the original Dynamo system.  
> DynamoDB uses **single-leader replication** internally (with a consensus algorithm like Multi-Paxos).

In a leaderless setup, reads and writes are typically coordinated using **quorums**.

---

## 6. Versioning and Conflict Resolution

In leaderless (and some leader-based) systems, each value typically has **version metadata**, such as:

- A **timestamp** (physical or logical).
- A **version vector** / **vector clock**.
- Or a combination of versions plus **application-level conflict resolution rules**.

When multiple replicas return different versions:

- The coordinator must:
  - **Reconcile** versions (e.g., pick the latest by timestamp, merge with CRDT rules, etc.).
  - Optionally perform **read repair**: write the latest version back to stale replicas in the background.

---

## 7. Quorums for Reading and Writing

Consider a replication factor of **`n` replicas** for a particular key.

- A write is considered **successful** if it is acknowledged by **`w` replicas**.
- A read consults **at least `r` replicas**.

We typically require:

> **`w + r > n`** (quorum condition)

Then, for any **successful write**:

- At least `w` replicas have the new value.
- A read that consults `r` replicas will **overlap** with those `w` replicas on **at least one node**.
- So, **at least one of the `r` replicas** must have the **latest value**.

### 7.1 Example (3-node cluster)

- `n = 3` replicas
- `w = 2` (write quorum of 2)
- `r = 2` (read quorum of 2)  
  → `w + r = 4 > 3 = n` ⇒ quorum condition is satisfied.

#### Successful Write

- Write is considered **committed** only after **2 replicas** acknowledge.
- This guarantees the value is stored on at least 2 nodes.

#### Read

Operationally, “read from `r` replicas” means:

1. The **coordinator** receives a read request.
2. It sends read requests to **some or all** of the `n` replicas (often all).
3. It **waits for responses from at least `r` replicas**.
4. Once it has `r` responses, it:
   - **Reconciles** different versions (if needed).
   - Picks the **most recent / authoritative** version according to its versioning scheme.
   - Returns that value to the client.
   - Optionally repairs stale replicas in the background (read repair).

Important:

- **Not all `r` responses have to be up to date.**
- The quorum condition only guarantees that **at least one** of them is up to date, assuming:
  - no extreme clock skew,
  - and the write actually succeeded with `w` acknowledgements.

---

## 8. Edge Cases and Anomalies

Even with quorums, anomalies can occur. These are best understood using the vocabulary in  
**[Database Transactions vs Distributed Systems Consistency](./database-transactions-vs-distributed-systems-consistency.md)**  
(see the sections on **consistency models** and **session guarantees** like **monotonic reads**).

### 8.1 Edge Case 1 — Stale Recovery

Scenario:

1. 3 replicas: Node 1, Node 2, Node 3.
2. Latest writes were successfully applied to **Node 1** and **Node 2**, but **Node 3** is **stale**.
3. Node 1 fails and is mistakenly **restored from Node 3’s stale data** (instead of catching up from Node 2).
4. Now **Node 1 and Node 3 both hold stale data**.

A read with quorum parameters `n = 3, w = 2, r = 2` might contact Node 1 and Node 3:

- Both return the **stale** value, **even though `w + r > n`**.

This is a **violated assumption** scenario:

- Quorum math assumes that **replicas don’t “go back in time”**.
- Restoring from a stale snapshot breaks that assumption.

### 8.2 Edge Case 2 — Write “Failure” That Partially Succeeds

Scenario:

1. Write is sent to all replicas, but only some of them acknowledge before timeout.
2. The system requires `w` acknowledgements, but fewer are received, so:
   - The client sees the write as **failed**.
   - **However**, some replicas may have actually **stored the new value**.

Later:

- Reads may or may not see that “failed” value, depending on which replicas respond.
- From the client’s perspective, this is confusing:
  - “The write failed, but sometimes I can read it.”

This is a classic **inconsistency between operation outcome and data state**.

### 8.3 Edge Case 3 — Concurrent Read and Write (Monotonic Reads Violation)

Scenario (3 replicas `A, B, C`):

- Initial value: `x = 0` on all replicas.
- Write `x = 1` starts:
  - It is written to **A only** so far (write is **in progress**, not yet a quorum commit).
- **Read 1**:
  - The coordinator queries replicas A and B.
  - A returns `x = 1` (new value), B returns `x = 0` (old).
  - Coordinator picks the latest → client sees **`x = 1`**.
- **Read 2** (later in real time):
  - The coordinator queries replicas B and C.
  - Both return `x = 0`.
  - Client now sees **`x = 0`** again.

From the client’s perspective:

- First read: sees **new** value `1`.
- Later read: sees **old** value `0`.

In the vocabulary of the earlier document:

- This is a **violation of “monotonic reads”** (a session guarantee).
- It also violates **linearizability**: a later read should not see an older value than an earlier read.

This happens because:

- The write was **still in progress**, not fully committed (`< w` replicas).
- Different reads hit **different subsets** of replicas with different states.

For more terminology and mapping to database isolation levels, see the sections on **session guarantees**, **linearizability**, and **stale reads** in:  
**[Database Transactions vs Distributed Systems Consistency](./database-transactions-vs-distributed-systems-consistency.md)**

---
