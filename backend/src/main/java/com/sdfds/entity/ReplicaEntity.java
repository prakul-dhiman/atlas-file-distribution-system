package com.sdfds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "replicas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_chunk_node", columnNames = {"chunk_id", "storage_node_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplicaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    private ChunkEntity chunk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_node_id", nullable = false)
    private StorageNodeEntity storageNode;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CORRUPTED, ORPHANED

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
