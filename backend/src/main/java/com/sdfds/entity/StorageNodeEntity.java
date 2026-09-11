package com.sdfds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "storage_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 100)
    private String nodeName;

    @Column(name = "endpoint_url", nullable = false)
    private String endpointUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "HEALTHY"; // HEALTHY, DOWN, DEGRADED

    @Column(name = "total_capacity_bytes", nullable = false)
    private Long totalCapacityBytes;

    @Column(name = "used_capacity_bytes", nullable = false)
    @Builder.Default
    private Long usedCapacityBytes = 0L;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
