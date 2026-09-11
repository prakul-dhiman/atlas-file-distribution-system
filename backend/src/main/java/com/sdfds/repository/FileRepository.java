package com.sdfds.repository;

import com.sdfds.entity.FileEntity;
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
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByUserAndFolderIsNullAndIsTrashedFalse(User user);

    List<FileEntity> findByUserAndFolderAndIsTrashedFalse(User user, Folder folder);

    @Query("""
            select f from FileEntity f
            where f.user = :user
              and f.folder is null
              and f.isTrashed = false
              and f.version = (
                  select max(f2.version) from FileEntity f2
                  where f2.user = :user
                    and f2.folder is null
                    and f2.name = f.name
                    and f2.isTrashed = false
              )
            """)
    List<FileEntity> findLatestActiveRootFiles(User user);

    @Query("""
            select f from FileEntity f
            where f.user = :user
              and f.folder = :folder
              and f.isTrashed = false
              and f.version = (
                  select max(f2.version) from FileEntity f2
                  where f2.user = :user
                    and f2.folder = :folder
                    and f2.name = f.name
                    and f2.isTrashed = false
              )
            """)
    List<FileEntity> findLatestActiveFilesInFolder(User user, Folder folder);

    List<FileEntity> findByUserAndFolder(User user, Folder folder);

    Optional<FileEntity> findByIdAndUser(Long id, User user);

    Optional<FileEntity> findByIdAndUserAndIsTrashedFalse(Long id, User user);

    List<FileEntity> findByUserAndNameContainingIgnoreCaseAndIsTrashedFalse(User user, String name);

    @Query("""
            select f from FileEntity f
            where f.user = :user
              and lower(f.name) like lower(concat('%', :name, '%'))
              and f.isTrashed = false
              and f.version = (
                  select max(f2.version) from FileEntity f2
                  where f2.user = :user
                    and f2.name = f.name
                    and ((f2.folder is null and f.folder is null) or f2.folder = f.folder)
                    and f2.isTrashed = false
              )
            """)
    List<FileEntity> searchLatestActiveFiles(User user, String name);

    boolean existsByUserAndNameAndFolderAndIsTrashedFalse(User user, String name, Folder folder);

    boolean existsByUserAndNameAndFolderIsNullAndIsTrashedFalse(User user, String name);

    List<FileEntity> findByUserAndNameAndFolderAndIsTrashedFalseOrderByVersionDesc(User user, String name, Folder folder);

    List<FileEntity> findByUserAndNameAndFolderIsNullAndIsTrashedFalseOrderByVersionDesc(User user, String name);

    List<FileEntity> findByUserAndNameAndFolderOrderByVersionDesc(User user, String name, Folder folder);

    List<FileEntity> findByUserAndNameAndFolderIsNullOrderByVersionDesc(User user, String name);

    List<FileEntity> findByUserAndIsTrashedTrue(User user);

    List<FileEntity> findByUserAndIsStarredTrueAndIsTrashedFalse(User user);

    Optional<FileEntity> findByIdAndUserAndIsTrashedTrue(Long id, User user);

    List<FileEntity> findByIsTrashedTrueAndTrashedAtBefore(Instant cutoff);

    List<FileEntity> findByFolder(Folder folder);
}
