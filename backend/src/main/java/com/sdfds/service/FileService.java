package com.sdfds.service;

import com.sdfds.dto.FileDto;
import com.sdfds.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileDto uploadFile(User user, MultipartFile file, Long folderId);

    FileDto getFileMetadata(User user, Long fileId);

    FileDownloadResource downloadFile(User user, Long fileId);

    FileDto renameFile(User user, Long fileId, String newName);

    FileDto moveFile(User user, Long fileId, Long targetFolderId);

    FileDto copyFile(User user, Long fileId, Long targetFolderId);

    void deleteFile(User user, Long fileId);

    java.util.List<FileDto> getFileVersionHistory(User user, Long fileId);

    FileDto restoreFileVersion(User user, Long fileId, Integer targetVersion);

    FileDto toggleStar(User user, Long fileId);

    java.util.List<FileDto> getStarredFiles(User user);

    byte[] downloadFilesAsZip(User user, java.util.List<Long> fileIds);

    record FileDownloadResource(Resource resource, String filename, String mimeType, long sizeBytes) {}
}
