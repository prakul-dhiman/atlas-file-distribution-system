package com.sdfds.storage.registry;

import com.sdfds.entity.StorageNodeEntity;
import com.sdfds.repository.StorageNodeRepository;
import com.sdfds.storage.node.SimulatedLocalStorageNode;
import com.sdfds.storage.node.StorageNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class StorageNodeRegistry {

    private static final Logger log = LoggerFactory.getLogger(StorageNodeRegistry.class);

    private final StorageNodeRepository storageNodeRepository;
    private final Map<Long, StorageNode> nodeInstanceMap = new HashMap<>();

    @Value("${storage.node-data-dir:./node_data}")
    private String nodeDataDir;

    @PostConstruct
    public synchronized void initializeNodes() {
        List<StorageNodeEntity> existingNodes = storageNodeRepository.findAll();

        if (existingNodes.isEmpty()) {
            log.info("No storage nodes registered in DB. Seeding 3 default simulated storage nodes...");
            for (int i = 1; i <= 3; i++) {
                String nodeName = "node-" + i;
                StorageNodeEntity nodeEntity = StorageNodeEntity.builder()
                        .nodeName(nodeName)
                        .endpointUrl("http://localhost:808" + i + "/storage")
                        .status("HEALTHY")
                        .totalCapacityBytes(107374182400L) // 100 GB
                        .usedCapacityBytes(0L)
                        .lastHeartbeat(Instant.now())
                        .build();
                existingNodes.add(storageNodeRepository.save(nodeEntity));
            }
        }

        for (StorageNodeEntity entity : existingNodes) {
            StorageNode node = new SimulatedLocalStorageNode(
                    entity.getId(),
                    entity.getNodeName(),
                    entity.getEndpointUrl(),
                    nodeDataDir,
                    entity.getTotalCapacityBytes()
            );
            node.setStatus(entity.getStatus());
            nodeInstanceMap.put(entity.getId(), node);
        }

        log.info("StorageNodeRegistry initialized with {} active node instances", nodeInstanceMap.size());
    }

    public StorageNode getNode(Long nodeId) {
        return nodeInstanceMap.get(nodeId);
    }

    public Collection<StorageNode> getAllNodes() {
        return nodeInstanceMap.values();
    }

    public List<StorageNode> selectNodesForReplication(int count, Set<Long> excludeNodeIds) {
        List<StorageNode> candidates = nodeInstanceMap.values().stream()
                .filter(StorageNode::healthCheck)
                .filter(node -> excludeNodeIds == null || !excludeNodeIds.contains(node.metadata().nodeId()))
                .sorted(Comparator.comparingLong((StorageNode n) -> n.metadata().availableCapacityBytes()).reversed())
                .limit(count)
                .toList();

        if (candidates.size() < count) {
            log.warn("Requested {} storage nodes for replication, but only {} healthy candidates available", count, candidates.size());
        }
        return candidates;
    }

    public synchronized void updateNodeStatus(Long nodeId, String status) {
        StorageNode node = nodeInstanceMap.get(nodeId);
        if (node != null) {
            node.setStatus(status);
            storageNodeRepository.findById(nodeId).ifPresent(entity -> {
                entity.setStatus(status);
                entity.setLastHeartbeat(Instant.now());
                storageNodeRepository.save(entity);
            });
            log.warn("Storage node ID: {} status updated to: {}", nodeId, status);
        }
    }
}
