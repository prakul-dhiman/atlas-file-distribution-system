package com.sdfds.storage.node;

import com.sdfds.storage.model.ChunkId;
import com.sdfds.storage.model.ChunkWriteResult;
import com.sdfds.storage.model.NodeMetadata;

import java.io.IOException;

public interface StorageNode {

    ChunkWriteResult writeChunk(ChunkId id, byte[] data, String expectedChecksum) throws IOException;

    byte[] readChunk(ChunkId id) throws IOException;

    boolean healthCheck();

    boolean deleteChunk(ChunkId id) throws IOException;

    NodeMetadata metadata();

    void setStatus(String status);
}
