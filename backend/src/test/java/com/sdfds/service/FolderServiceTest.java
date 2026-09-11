package com.sdfds.service;

import com.sdfds.dto.CreateFolderRequest;
import com.sdfds.dto.FolderDto;
import com.sdfds.entity.Folder;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.mapper.FolderMapper;
import com.sdfds.repository.FileRepository;
import com.sdfds.repository.FolderRepository;
import com.sdfds.service.impl.FolderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private FileRepository fileRepository;

    @Spy
    private FolderMapper folderMapper;

    @Spy
    private FileMapper fileMapper;

    @InjectMocks
    private FolderServiceImpl folderService;

    private User testUser;
    private Folder rootFolder;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").build();
        rootFolder = Folder.builder().id(10L).name("Documents").user(testUser).isTrashed(false).build();
    }

    @Test
    @DisplayName("Create Root Folder - Success")
    void createFolder_Root_Success() {
        CreateFolderRequest request = CreateFolderRequest.builder().name("Documents").build();

        when(folderRepository.existsByUserAndNameAndParentIsNullAndIsTrashedFalse(testUser, "Documents")).thenReturn(false);
        when(folderRepository.save(any(Folder.class))).thenReturn(rootFolder);

        FolderDto result = folderService.createFolder(testUser, request);

        assertNotNull(result);
        assertEquals("Documents", result.getName());
        assertNull(result.getParentId());
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("Create Duplicate Folder - Fails")
    void createFolder_Duplicate_Fails() {
        CreateFolderRequest request = CreateFolderRequest.builder().name("Documents").build();

        when(folderRepository.existsByUserAndNameAndParentIsNullAndIsTrashedFalse(testUser, "Documents")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolder(testUser, request));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Rename Folder - Success")
    void renameFolder_Success() {
        when(folderRepository.findByIdAndUserAndIsTrashedFalse(10L, testUser)).thenReturn(Optional.of(rootFolder));
        when(folderRepository.existsByUserAndNameAndParentIsNullAndIsTrashedFalse(testUser, "Archives")).thenReturn(false);
        when(folderRepository.save(any(Folder.class))).thenAnswer(i -> i.getArgument(0));

        FolderDto renamed = folderService.renameFolder(testUser, 10L, "Archives");

        assertNotNull(renamed);
        assertEquals("Archives", renamed.getName());
    }
}
