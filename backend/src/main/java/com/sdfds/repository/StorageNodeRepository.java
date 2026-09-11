package com.sdfds.repository;

import com.sdfds.entity.StorageNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageNodeRepository extends JpaRepository<StorageNodeEntity, Long> {

    Optional<StorageNodeEntity> findByNodeName(String nodeName);

    List<StorageNodeEntity> findByStatus(String status);
}
