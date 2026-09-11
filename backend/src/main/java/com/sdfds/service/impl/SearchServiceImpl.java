package com.sdfds.service.impl;

import com.sdfds.dto.FileDto;
import com.sdfds.dto.FolderDto;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.mapper.FolderMapper;
import com.sdfds.repository.FileRepository;
import com.sdfds.repository.FolderRepository;
import com.sdfds.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FileMapper fileMapper;
    private final FolderMapper folderMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FolderDto> searchFolders(User user, String query) {
        log.info("Searching folders for user ID: {} with query: '{}'", user.getId(), query);

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        return folderRepository.findByUserAndNameContainingIgnoreCaseAndIsTrashedFalse(user, query.trim())
                .stream()
                .map(folderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDto> searchFiles(User user, String query) {
        log.info("Searching files for user ID: {} with query: '{}'", user.getId(), query);

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        return fileRepository.searchLatestActiveFiles(user, query.trim())
                .stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }
}
