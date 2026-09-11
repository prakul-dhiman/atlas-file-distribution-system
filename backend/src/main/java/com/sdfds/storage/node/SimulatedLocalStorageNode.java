package com.sdfds.storage.node;

import com.sdfds.storage.model.ChunkId;
import com.sdfds.storage.model.ChunkWriteResult;
import com.sdfds.storage.model.NodeMetadata;
import com.sdfds.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SimulatedLocalStorageNode implements StorageNode {

    private static final Logger log = LoggerFactory.getLogger(SimulatedLocalStorageNode.class);

    private final Long nodeId;
    private final String nodeName;
    private final String endpointUrl;
    private final Path nodeBasePath;
    private final long totalCapacityBytes;
    private long usedCapacityBytes;
    private String status; // HEALTHY, DOWN, DEGRADED

    public SimulatedLocalStorageNode(Long nodeId, String nodeName, String endpointUrl, String baseDirPath, long totalCapacityBytes) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.endpointUrl = endpointUrl;
        this.nodeBasePath = Paths.get(baseDirPath).resolve(nodeName).toAbsolutePath().normalize();
        this.totalCapacityBytes = totalCapacityBytes;
        this.usedCapacityBytes = 0;
        this.status = "HEALTHY";

        try {
            Files.createDirectories(this.nodeBasePath);
            log.info("Initialized SimulatedLocalStorageNode [{}] at path: {}", nodeName, this.nodeBasePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create node base path for " + nodeName, e);
        }
    }

    @Override
    public synchronized ChunkWriteResult writeChunk(ChunkId id, byte[] data, String expectedChecksum) throws IOException {
        if (!"HEALTHY".equalsIgnoreCase(status)) {
            throw new IOException("Storage node [" + nodeName + "] is currently " + status);
        }

        Path chunkPath = nodeBasePath.resolve(id.toString() + ".chk");
        Files.write(chunkPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        String actualChecksum = calculateSha256(data);
        if (expectedChecksum != null && !expectedChecksum.equalsIgnoreCase(actualChecksum)) {
            Files.deleteIfExists(chunkPath);
            throw new IOException("Checksum mismatch writing chunk " + id + " on node " + nodeName);
        }

        this.usedCapacityBytes += data.length;
        log.info("Node [{}] wrote chunk {} (size: {} bytes, path: {})", nodeName, id, data.length, chunkPath);

        return new ChunkWriteResult(id, nodeId, chunkPath.toString(), data.length, actualChecksum);
    }

    @Override
    public byte[] readChunk(ChunkId id) throws IOException {
        if (!"HEALTHY".equalsIgnoreCase(status)) {
            throw new IOException("Storage node [" + nodeName + "] is currently " + status);
        }

        Path chunkPath = nodeBasePath.resolve(id.toString() + ".chk");
        if (!Files.exists(chunkPath)) {
            throw new FileNotFoundException("Chunk " + id + " not found on node " + nodeName);
        }

        byte[] data = Files.readAllBytes(chunkPath);
        log.info("Node [{}] read chunk {} (size: {} bytes)", nodeName, id, data.length);
        return data;
    }

    @Override
    public boolean healthCheck() {
        return "HEALTHY".equalsIgnoreCase(status) && Files.exists(nodeBasePath);
    }

    @Override
    public synchronized boolean deleteChunk(ChunkId id) throws IOException {
        Path chunkPath = nodeBasePath.resolve(id.toString() + ".chk");
        boolean deleted = Files.deleteIfExists(chunkPath);
        if (deleted) {
            log.info("Node [{}] deleted chunk {} (path: {})", nodeName, id, chunkPath);
        } else {
            log.warn("Node [{}] chunk file not found for deletion: {}", nodeName, chunkPath);
        }
        return deleted;
    }

    @Override
    public NodeMetadata metadata() {
        return new NodeMetadata(nodeId, nodeName, endpointUrl, status, totalCapacityBytes, usedCapacityBytes);
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
        log.warn("Storage node [{}] status updated to: {}", nodeName, status);
    }

    private String calculateSha256(byte[] data) {
        return HashUtils.sha256Hex(data);
    }
}
