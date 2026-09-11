package com.sdfds.repository;

import com.sdfds.entity.DirectItemShare;
import com.sdfds.entity.FileEntity;
import com.sdfds.entity.Folder;
import com.sdfds.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectItemShareRepository extends JpaRepository<DirectItemShare, Long> {

    List<DirectItemShare> findBySharedWithUser(User user);

    List<DirectItemShare> findByFile(FileEntity file);

    List<DirectItemShare> findByFolder(Folder folder);

    Optional<DirectItemShare> findBySharedWithUserAndFile(User sharedWithUser, FileEntity file);

    Optional<DirectItemShare> findBySharedWithUserAndFolder(User sharedWithUser, Folder folder);

    void deleteByFile(FileEntity file);

    void deleteByFolder(Folder folder);
}
