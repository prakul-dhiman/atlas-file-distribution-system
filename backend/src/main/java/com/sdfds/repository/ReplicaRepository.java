package com.sdfds.repository;

import com.sdfds.entity.ChunkEntity;
import com.sdfds.entity.ReplicaEntity;
import com.sdfds.entity.StorageNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplicaRepository extends JpaRepository<ReplicaEntity, Long> {

    List<ReplicaEntity> findByChunk(ChunkEntity chunk);

    List<ReplicaEntity> findByChunkAndStatus(ChunkEntity chunk, String status);

    List<ReplicaEntity> findByStorageNode(StorageNodeEntity storageNode);
}
