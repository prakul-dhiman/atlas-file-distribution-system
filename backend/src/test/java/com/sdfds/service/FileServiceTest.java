package com.sdfds.service;

import com.sdfds.dto.FileDto;
import com.sdfds.entity.FileEntity;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.repository.FileRepository;
import com.sdfds.repository.FolderRepository;
import com.sdfds.repository.UserRepository;
import com.sdfds.service.impl.FileServiceImpl;
import com.sdfds.storage.LocalStorageService;
import com.sdfds.storage.distribution.ChunkDistributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalStorageService localStorageService;

    @Mock
    private ChunkDistributionService chunkDistributionService;

    @Spy
    private FileMapper fileMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .storageQuotaBytes(5368709120L) // 5GB
                .usedStorageBytes(0L)
                .build();
    }

    @Test
    @DisplayName("Upload File - Success")
    void uploadFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());

        FileEntity initialEntity = FileEntity.builder().id(100L).name("test.txt").sizeBytes(11L).user(testUser).build();
        when(fileRepository.save(any(FileEntity.class))).thenReturn(initialEntity);
        when(localStorageService.saveFile(eq(1L), eq(100L), any(InputStream.class)))
                .thenReturn(new LocalStorageService.WriteResult(11L, "dummy_checksum_sha256"));

        FileDto result = fileService.uploadFile(testUser, file, null);

        assertNotNull(result);
        assertEquals("test.txt", result.getName());
        verify(localStorageService).saveFile(eq(1L), eq(100L), any(InputStream.class));
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Upload File - Fails when quota exceeded")
    void uploadFile_QuotaExceeded() {
        testUser.setUsedStorageBytes(5368709100L); // Nearly full
        MockMultipartFile file = new MockMultipartFile("file", "large.bin", "application/octet-stream", new byte[1000]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileService.uploadFile(testUser, file, null));

        assertTrue(ex.getMessage().contains("Storage quota exceeded"));
    }

    @Test
    @DisplayName("Soft Delete File - Success")
    void deleteFile_Success() {
        FileEntity fileEntity = FileEntity.builder().id(100L).name("test.txt").user(testUser).isTrashed(false).build();
        when(fileRepository.findByIdAndUserAndIsTrashedFalse(100L, testUser)).thenReturn(Optional.of(fileEntity));

        fileService.deleteFile(testUser, 100L);

        assertTrue(fileEntity.getIsTrashed());
        assertNotNull(fileEntity.getTrashedAt());
        verify(fileRepository).save(fileEntity);
    }
}
