# ADR-0004: Phase 3 Chunked Storage and Simulated Distribution Design

- **Status**: Accepted
- **Date**: 2026-07-27
- **Context**: The Smart Distributed File Distribution System (SDFDS) requires distributed chunking, multi-node replication, integrity verification, and fault recovery for large files.

## Design Rationale
1. **Chunking Threshold & Size**: Files $> 100 \text{ MB}$ (`storage.direct-upload-max-bytes`) are automatically split into fixed 8 MB chunks (`storage.chunk-size-bytes` = $8,388,608 \text{ bytes}$). Files $\le 100 \text{ MB}$ bypass chunking and use direct single-node storage for lower complexity.
2. **Replication Policy**: Each 8 MB chunk is assigned a unique SHA-256 hash and written to a minimum of 2 distinct active storage nodes (`storage.replication-factor` = 2) chosen by highest available disk capacity.
3. **Storage Node Abstraction**: Nodes implement the `StorageNode` contract (`writeChunk`, `readChunk`, `healthCheck`, `metadata`). In this phase, nodes are simulated as isolated directory structures (`./node_data/node-1/`, `./node_data/node-2/`, `./node_data/node-3/`).
4. **Dynamic Node Failover**: During download of a chunked file, the reader streams chunks sequentially from replica nodes. If the primary storage node holding a chunk replica is `DOWN` or returns an I/O error, the stream transparently fails over to an active secondary replica node.
5. **Periodic Self-Healing**: A background scheduler (`StorageRecoveryScheduler`) runs every 10 seconds. It checks storage node health and scans for under-replicated chunks (active replicas $< 2$). It automatically re-replicates missing chunk copies from surviving healthy replicas to available active nodes.

## Acceptance Criteria Satisfied
1. Files $> 100 \text{ MB}$ are chunked into 8 MB blocks with individual SHA-256 checksums.
2. Each chunk is saved to $\ge 2$ distinct storage nodes.
3. Simulated node failure (`node status = DOWN`) mid-download does not interrupt file delivery; failover to secondary replica serves content correctly.
4. Auto-healing scheduler detects node failures and restores replication factor to 2 across healthy nodes.
