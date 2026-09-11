package com.sdfds.repository;

import com.sdfds.entity.Folder;
import com.sdfds.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUserAndParentIsNullAndIsTrashedFalse(User user);

    List<Folder> findByUserAndParentAndIsTrashedFalse(User user, Folder parent);

    List<Folder> findByUserAndParent(User user, Folder parent);

    Optional<Folder> findByIdAndUser(Long id, User user);

    Optional<Folder> findByIdAndUserAndIsTrashedFalse(Long id, User user);

    List<Folder> findByUserAndNameContainingIgnoreCaseAndIsTrashedFalse(User user, String name);

    boolean existsByUserAndNameAndParentAndIsTrashedFalse(User user, String name, Folder parent);

    boolean existsByUserAndNameAndParentIsNullAndIsTrashedFalse(User user, String name);

    List<Folder> findByUserAndIsTrashedTrue(User user);

    Optional<Folder> findByIdAndUserAndIsTrashedTrue(Long id, User user);

    List<Folder> findByIsTrashedTrueAndTrashedAtBefore(Instant cutoff);

    List<Folder> findByParent(Folder parent);
}
