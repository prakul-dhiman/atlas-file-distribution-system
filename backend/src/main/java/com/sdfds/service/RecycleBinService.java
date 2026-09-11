package com.sdfds.service;

import com.sdfds.dto.FileDto;
import com.sdfds.dto.FolderDto;
import com.sdfds.dto.RecycleBinResponse;
import com.sdfds.entity.User;

public interface RecycleBinService {

    RecycleBinResponse listTrashedItems(User user);

    FolderDto restoreFolder(User user, Long folderId);

    FileDto restoreFile(User user, Long fileId);

    void purgeFolder(User user, Long folderId);

    void purgeFile(User user, Long fileId);

    void emptyTrash(User user);

    void autoCleanup();
}
