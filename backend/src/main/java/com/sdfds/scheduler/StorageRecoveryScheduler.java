package com.sdfds.scheduler;

import com.sdfds.entity.*;
import com.sdfds.repository.*;
import com.sdfds.storage.model.ChunkId;
import com.sdfds.storage.model.ChunkWriteResult;
import com.sdfds.storage.node.StorageNode;
import com.sdfds.storage.registry.StorageNodeRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StorageRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(StorageRecoveryScheduler.class);

    private final StorageNodeRegistry nodeRegistry;
    private final ChunkRepository chunkRepository;
    private final ReplicaRepository replicaRepository;
    private final StorageNodeRepository storageNodeRepository;

    private final Map<Long, Integer> nodeFailureCountMap = new HashMap<>();

    @Value("${storage.replication-factor:2}")
    private int requiredReplicationFactor;

    @Scheduled(fixedDelay = 10000) // 10 seconds
    @Transactional
    public void performNodeHealthAndReplicaRecoverySweep() {
        log.debug("Running StorageRecoveryScheduler sweep...");

        // 1. Health check sweep across registered nodes
        for (StorageNode node : nodeRegistry.getAllNodes()) {
            Long nodeId = node.metadata().nodeId();
            boolean isHealthy = node.healthCheck();

            if (!isHealthy) {
                int failures = nodeFailureCountMap.getOrDefault(nodeId, 0) + 1;
                nodeFailureCountMap.put(nodeId, failures);
                log.warn("Node [{}] failed health check (consecutive failures: {})", node.metadata().nodeName(), failures);

                if (failures >= 3 && !"DOWN".equalsIgnoreCase(node.metadata().status())) {
                    nodeRegistry.updateNodeStatus(nodeId, "DOWN");
                    log.error("Storage node [{}] marked DOWN after 3 consecutive failures", node.metadata().nodeName());
                }
            } else {
                nodeFailureCountMap.put(nodeId, 0);
            }
        }

        // 2. Recovery sweep for under-replicated chunks
        List<ChunkEntity> allChunks = chunkRepository.findAll();
        for (ChunkEntity chunk : allChunks) {
            List<ReplicaEntity> activeReplicas = replicaRepository.findByChunkAndStatus(chunk, "ACTIVE").stream()
                    .filter(r -> "HEALTHY".equalsIgnoreCase(r.getStorageNode().getStatus()))
                    .toList();

            if (activeReplicas.size() < requiredReplicationFactor && !activeReplicas.isEmpty()) {
                log.warn("Chunk ID {} (index {}) is under-replicated! Active healthy replicas: {}, required: {}",
                        chunk.getId(), chunk.getChunkIndex(), activeReplicas.size(), requiredReplicationFactor);

                reReplicateChunk(chunk, activeReplicas);
            }
        }
    }

    private void reReplicateChunk(ChunkEntity chunk, List<ReplicaEntity> healthyReplicas) {
        ReplicaEntity sourceReplica = healthyReplicas.get(0);
        StorageNode sourceNode = nodeRegistry.getNode(sourceReplica.getStorageNode().getId());

        if (sourceNode == null || !sourceNode.healthCheck()) {
            log.error("Cannot recover chunk ID {} because source node is unavailable", chunk.getId());
            return;
        }

        Set<Long> existingNodeIds = healthyReplicas.stream()
                .map(r -> r.getStorageNode().getId())
                .collect(Collectors.toSet());

        List<StorageNode> targetNodes = nodeRegistry.selectNodesForReplication(1, existingNodeIds);
        if (targetNodes.isEmpty()) {
            log.error("No target node available for re-replicating chunk ID {}", chunk.getId());
            return;
        }

        StorageNode targetNode = targetNodes.get(0);
        ChunkId chunkId = new ChunkId(chunk.getFile().getId(), chunk.getChunkIndex());

        try {
            byte[] chunkData = sourceNode.readChunk(chunkId);
            ChunkWriteResult result = targetNode.writeChunk(chunkId, chunkData, chunk.getChecksumSha256());

            StorageNodeEntity targetNodeEntity = storageNodeRepository.findById(targetNode.metadata().nodeId()).orElseThrow();

            ReplicaEntity newReplica = ReplicaEntity.builder()
                    .chunk(chunk)
                    .storageNode(targetNodeEntity)
                    .storagePath(result.storagePath())
                    .status("ACTIVE")
                    .build();

            replicaRepository.save(newReplica);
            log.info("Successfully re-replicated chunk {} to node [{}]! Replica count restored.",
                    chunkId, targetNodeEntity.getNodeName());

        } catch (IOException e) {
            log.error("Failed to re-replicate chunk {} to node [{}]", chunkId, targetNode.metadata().nodeName(), e);
        }
    }
}
