package com.sdfds.storage.model;

public record ChunkId(Long fileId, int chunkIndex) {
    @Override
    public String toString() {
        return fileId + "_chunk_" + chunkIndex;
    }
}
