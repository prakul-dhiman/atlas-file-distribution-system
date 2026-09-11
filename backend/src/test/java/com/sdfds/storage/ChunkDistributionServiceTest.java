package com.sdfds.storage;

import com.sdfds.entity.*;
import com.sdfds.repository.*;
import com.sdfds.storage.distribution.ChunkDistributionService;
import com.sdfds.storage.node.SimulatedLocalStorageNode;
import com.sdfds.storage.node.StorageNode;
import com.sdfds.storage.registry.StorageNodeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class ChunkDistributionServiceTest {

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ReplicaRepository replicaRepository;

    @Mock
    private StorageNodeRepository storageNodeRepository;

    @Mock
    private StorageNodeRegistry nodeRegistry;

    @InjectMocks
    private ChunkDistributionService chunkDistributionService;

    @TempDir
    Path tempDir;

    private FileEntity testFile;
    private StorageNode node1;
    private StorageNode node2;
    private StorageNodeEntity nodeEntity1;
    private StorageNodeEntity nodeEntity2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chunkDistributionService, "chunkSize", 1024); // 1KB test chunks
        ReflectionTestUtils.setField(chunkDistributionService, "replicationFactor", 2);

        testFile = FileEntity.builder().id(50L).name("large_file.bin").build();

        node1 = new SimulatedLocalStorageNode(1L, "node-1", "http://node1", tempDir.toString(), 1000000L);
        node2 = new SimulatedLocalStorageNode(2L, "node-2", "http://node2", tempDir.toString(), 1000000L);

        nodeEntity1 = StorageNodeEntity.builder().id(1L).nodeName("node-1").usedCapacityBytes(0L).build();
        nodeEntity2 = StorageNodeEntity.builder().id(2L).nodeName("node-2").usedCapacityBytes(0L).build();
    }

    @Test
    @DisplayName("Split and Distribute - Slices stream into chunks and replicates to 2 nodes")
    void splitAndDistribute_Success() throws Exception {
        byte[] payload = new byte[2500]; // 3 chunks (1024 + 1024 + 452)
        InputStream inputStream = new ByteArrayInputStream(payload);

        when(nodeRegistry.selectNodesForReplication(2, null)).thenReturn(List.of(node1, node2));
        when(chunkRepository.save(any(ChunkEntity.class))).thenAnswer(i -> {
            ChunkEntity c = i.getArgument(0);
            c.setId(100L + c.getChunkIndex());
            return c;
        });
        when(storageNodeRepository.findById(1L)).thenReturn(Optional.of(nodeEntity1));
        when(storageNodeRepository.findById(2L)).thenReturn(Optional.of(nodeEntity2));

        String checksum = chunkDistributionService.splitAndDistribute(testFile, inputStream);

        assertNotNull(checksum);
        assertEquals(2500L, testFile.getSizeBytes());
        assertTrue(testFile.getIsChunked());
        verify(chunkRepository, times(3)).save(any(ChunkEntity.class));
        verify(replicaRepository, times(6)).save(any(ReplicaEntity.class)); // 3 chunks * 2 replicas
    }
}
