package com.sdfds.storage.model;

public record NodeMetadata(
        Long nodeId,
        String nodeName,
        String endpointUrl,
        String status,
        long totalCapacityBytes,
        long usedCapacityBytes
) {
    public long availableCapacityBytes() {
        return Math.max(0, totalCapacityBytes - usedCapacityBytes);
    }
}
