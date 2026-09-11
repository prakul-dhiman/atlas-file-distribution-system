package com.sdfds.service;

import com.sdfds.dto.*;
import com.sdfds.entity.User;

import java.util.List;

public interface FolderService {

    FolderDto createFolder(User user, CreateFolderRequest request);

    FolderContentResponse getFolderContents(User user, Long folderId);

    List<FolderDto> getSubfolders(User user, Long parentFolderId);

    FolderDto renameFolder(User user, Long folderId, String newName);

    FolderDto moveFolder(User user, Long folderId, Long targetParentId);

    void deleteFolder(User user, Long folderId);
}
