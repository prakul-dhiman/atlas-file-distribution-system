package com.sdfds.storage;

import com.sdfds.entity.*;
import com.sdfds.repository.*;
import com.sdfds.scheduler.StorageRecoveryScheduler;
import com.sdfds.storage.distribution.ChunkDistributionService;
import com.sdfds.storage.node.SimulatedLocalStorageNode;
import com.sdfds.storage.node.StorageNode;
import com.sdfds.storage.registry.StorageNodeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeFailureRecoveryTest {

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ReplicaRepository replicaRepository;

    @Mock
    private StorageNodeRepository storageNodeRepository;

    @Mock
    private StorageNodeRegistry nodeRegistry;

    private ChunkDistributionService chunkDistributionService;
    private StorageRecoveryScheduler recoveryScheduler;

    @TempDir
    Path tempDir;

    private FileEntity testFile;
    private StorageNode node1;
    private StorageNode node2;
    private StorageNode node3;
    private StorageNodeEntity nodeEntity1;
    private StorageNodeEntity nodeEntity2;
    private StorageNodeEntity nodeEntity3;

    @BeforeEach
    void setUp() {
        chunkDistributionService = new ChunkDistributionService(chunkRepository, replicaRepository, storageNodeRepository, nodeRegistry);
        recoveryScheduler = new StorageRecoveryScheduler(nodeRegistry, chunkRepository, replicaRepository, storageNodeRepository);

        ReflectionTestUtils.setField(chunkDistributionService, "chunkSize", 1024);
        ReflectionTestUtils.setField(chunkDistributionService, "replicationFactor", 2);
        ReflectionTestUtils.setField(recoveryScheduler, "requiredReplicationFactor", 2);

        testFile = FileEntity.builder().id(99L).name("failover_test.bin").isChunked(true).build();

        node1 = new SimulatedLocalStorageNode(1L, "node-1", "http://node1", tempDir.toString(), 1000000L);
        node2 = new SimulatedLocalStorageNode(2L, "node-2", "http://node2", tempDir.toString(), 1000000L);
        node3 = new SimulatedLocalStorageNode(3L, "node-3", "http://node3", tempDir.toString(), 1000000L);

        nodeEntity1 = StorageNodeEntity.builder().id(1L).nodeName("node-1").status("HEALTHY").build();
        nodeEntity2 = StorageNodeEntity.builder().id(2L).nodeName("node-2").status("HEALTHY").build();
        nodeEntity3 = StorageNodeEntity.builder().id(3L).nodeName("node-3").status("HEALTHY").build();
    }

    @Test
    @DisplayName("Failover Download - Serves file from secondary replica when node-1 is DOWN")
    void assembleFileStream_Failover_Success() throws Exception {
        byte[] payload = "Hello Atlas Distributed World!".getBytes();

        when(nodeRegistry.selectNodesForReplication(2, null)).thenReturn(List.of(node1, node2));
        when(chunkRepository.save(any())).thenAnswer(i -> {
            ChunkEntity c = i.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(storageNodeRepository.findById(1L)).thenReturn(Optional.of(nodeEntity1));
        when(storageNodeRepository.findById(2L)).thenReturn(Optional.of(nodeEntity2));

        chunkDistributionService.splitAndDistribute(testFile, new ByteArrayInputStream(payload));

        ChunkEntity chunkEntity = ChunkEntity.builder().id(10L).file(testFile).chunkIndex(0).sizeBytes((long) payload.length).build();
        ReplicaEntity r1 = ReplicaEntity.builder().chunk(chunkEntity).storageNode(nodeEntity1).status("ACTIVE").build();
        ReplicaEntity r2 = ReplicaEntity.builder().chunk(chunkEntity).storageNode(nodeEntity2).status("ACTIVE").build();

        when(chunkRepository.findByFileOrderByChunkIndexAsc(testFile)).thenReturn(List.of(chunkEntity));
        when(replicaRepository.findByChunkAndStatus(chunkEntity, "ACTIVE")).thenReturn(List.of(r1, r2));

        // Simulate killing node-1
        node1.setStatus("DOWN");
        when(nodeRegistry.getNode(1L)).thenReturn(node1);
        when(nodeRegistry.getNode(2L)).thenReturn(node2);

        Resource downloadedResource = chunkDistributionService.assembleFileStream(testFile);

        assertNotNull(downloadedResource);
        assertArrayEquals(payload, downloadedResource.getInputStream().readAllBytes());
    }
}
