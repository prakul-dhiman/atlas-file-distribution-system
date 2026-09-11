package com.sdfds.storage.model;

public record ChunkWriteResult(
        ChunkId chunkId,
        Long storageNodeId,
        String storagePath,
        long bytesWritten,
        String checksumSha256
) {}
