package com.sdfds.service;

import com.sdfds.dto.FileDto;
import com.sdfds.dto.FolderDto;
import com.sdfds.entity.User;

import java.util.List;

public interface SearchService {

    List<FolderDto> searchFolders(User user, String query);

    List<FileDto> searchFiles(User user, String query);
}
