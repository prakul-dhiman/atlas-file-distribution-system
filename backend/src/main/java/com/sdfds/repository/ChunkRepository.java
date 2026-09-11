package com.sdfds.repository;

import com.sdfds.entity.ChunkEntity;
import com.sdfds.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<ChunkEntity, Long> {

    List<ChunkEntity> findByFileOrderByChunkIndexAsc(FileEntity file);

    void deleteByFile(FileEntity file);
}
