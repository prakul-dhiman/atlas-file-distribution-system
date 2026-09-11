package com.sdfds.storage.distribution;

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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChunkDistributionService {

    private static final Logger log = LoggerFactory.getLogger(ChunkDistributionService.class);

    private final ChunkRepository chunkRepository;
    private final ReplicaRepository replicaRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final StorageNodeRegistry nodeRegistry;

    @Value("${storage.chunk-size-bytes:8388608}") // 8 MB
    private int chunkSize;

    @Value("${storage.replication-factor:2}")
    private int replicationFactor;

    @Transactional
    public String splitAndDistribute(FileEntity fileEntity, InputStream inputStream) throws IOException {
        log.info("Starting chunk distribution for file ID: {}, name: '{}'", fileEntity.getId(), fileEntity.getName());

        byte[] buffer = new byte[chunkSize];
        int chunkIndex = 0;
        long totalBytesWritten = 0;

        MessageDigest overallDigest;
        try {
            overallDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }

        int bytesRead;
        while ((bytesRead = readFully(inputStream, buffer)) > 0) {
            byte[] chunkData = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

            overallDigest.update(chunkData, 0, bytesRead);
            String chunkChecksum = calculateSha256(chunkData);

            ChunkEntity chunkEntity = ChunkEntity.builder()
                    .file(fileEntity)
                    .chunkIndex(chunkIndex)
                    .sizeBytes((long) bytesRead)
                    .checksumSha256(chunkChecksum)
                    .build();

            ChunkEntity savedChunk = chunkRepository.save(chunkEntity);
            ChunkId chunkId = new ChunkId(fileEntity.getId(), chunkIndex);

            // Select 2 healthy nodes for replication
            List<StorageNode> selectedNodes = nodeRegistry.selectNodesForReplication(replicationFactor, null);
            if (selectedNodes.size() < replicationFactor) {
                throw new IllegalStateException("Insufficient healthy storage nodes available for replication factor " + replicationFactor);
            }

            for (StorageNode targetNode : selectedNodes) {
                ChunkWriteResult writeResult = targetNode.writeChunk(chunkId, chunkData, chunkChecksum);

                StorageNodeEntity nodeEntity = storageNodeRepository.findById(targetNode.metadata().nodeId())
                        .orElseThrow(() -> new IllegalStateException("Storage node entity missing in DB"));

                ReplicaEntity replicaEntity = ReplicaEntity.builder()
                        .chunk(savedChunk)
                        .storageNode(nodeEntity)
                        .storagePath(writeResult.storagePath())
                        .status("ACTIVE")
                        .build();

                replicaRepository.save(replicaEntity);
                nodeEntity.setUsedCapacityBytes(nodeEntity.getUsedCapacityBytes() + bytesRead);
                storageNodeRepository.save(nodeEntity);
            }

            totalBytesWritten += bytesRead;
            chunkIndex++;
        }

        String overallChecksum = toHexString(overallDigest.digest());
        fileEntity.setSizeBytes(totalBytesWritten);
        fileEntity.setIsChunked(true);
        fileEntity.setChecksumSha256(overallChecksum);

        log.info("Completed chunk distribution for file ID: {}. Total chunks: {}, size: {} bytes, SHA-256: {}",
                fileEntity.getId(), chunkIndex, totalBytesWritten, overallChecksum);

        return overallChecksum;
    }

    @Transactional(readOnly = true)
    public ByteArrayResource assembleFileStream(FileEntity fileEntity) throws IOException {
        log.info("Preparing chunk stream for file ID: {}", fileEntity.getId());

        List<ChunkEntity> chunks = chunkRepository.findByFileOrderByChunkIndexAsc(fileEntity);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No chunk records found for file ID: " + fileEntity.getId());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (ChunkEntity chunk : chunks) {
            ChunkId chunkId = new ChunkId(fileEntity.getId(), chunk.getChunkIndex());
            List<ReplicaEntity> replicas = replicaRepository.findByChunkAndStatus(chunk, "ACTIVE");
            if (replicas.isEmpty()) {
                throw new IllegalStateException("No active replica found for chunk " + chunkId);
            }

            byte[] chunkData = null;
            IOException lastException = null;
            for (ReplicaEntity replica : replicas) {
                StorageNode node = nodeRegistry.getNode(replica.getStorageNode().getId());
                if (node != null && node.healthCheck()) {
                    try {
                        chunkData = node.readChunk(chunkId);
                        break;
                    } catch (IOException e) {
                        lastException = e;
                    }
                }
            }

            if (chunkData == null) {
                throw new IOException("Failed to read chunk " + chunkId + " from any active replica", lastException);
            }

            outputStream.write(chunkData);
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }

    @Transactional
    public void deleteChunksForFile(FileEntity fileEntity) {
        log.info("Deleting all chunks and replicas for file ID: {}", fileEntity.getId());

        List<ChunkEntity> chunks = chunkRepository.findByFileOrderByChunkIndexAsc(fileEntity);
        for (ChunkEntity chunk : chunks) {
            ChunkId chunkId = new ChunkId(fileEntity.getId(), chunk.getChunkIndex());
            List<ReplicaEntity> replicas = replicaRepository.findByChunk(chunk);

            for (ReplicaEntity replica : replicas) {
                StorageNode node = nodeRegistry.getNode(replica.getStorageNode().getId());
                if (node != null) {
                    try {
                        node.deleteChunk(chunkId);
                    } catch (Exception e) {
                        log.warn("Failed to delete chunk {} from node [{}]", chunkId, replica.getStorageNode().getNodeName(), e);
                    }
                }

                StorageNodeEntity nodeEntity = replica.getStorageNode();
                nodeEntity.setUsedCapacityBytes(Math.max(0, nodeEntity.getUsedCapacityBytes() - chunk.getSizeBytes()));
                storageNodeRepository.save(nodeEntity);
            }

            replicaRepository.deleteAll(replicas);
        }

        chunkRepository.deleteByFile(fileEntity);
        log.info("Deleted {} chunks for file ID: {}", chunks.size(), fileEntity.getId());
    }

    private int readFully(InputStream in, byte[] b) throws IOException {
        int offset = 0;
        int len = b.length;
        while (offset < len) {
            int count = in.read(b, offset, len - offset);
            if (count < 0) {
                break;
            }
            offset += count;
        }
        return offset;
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String toHexString(byte[] bytes) {
        return bytesToHex(bytes);
    }
}
