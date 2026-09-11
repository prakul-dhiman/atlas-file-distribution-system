package com.sdfds.service;

import com.sdfds.dto.CreateSharedLinkRequest;
import com.sdfds.dto.SharedLinkDto;
import com.sdfds.entity.User;

import java.util.List;

public interface ShareService {

    SharedLinkDto createShareLink(User user, CreateSharedLinkRequest request);

    SharedLinkDto getSharedLinkMetadata(String token);

    Object getSharedLinkContents(String token, String password);

    String generateSignedDownloadUrl(String token, Long fileId, String password);

    boolean verifySignature(String token, Long fileId, long expires, String signature);

    FileService.FileDownloadResource downloadSharedFile(
            String token, Long fileId, long expires, String signature,
            String ipAddress, String userAgent);

    List<SharedLinkDto> listUserShares(User user);

    void revokeShare(User user, Long shareId);
}
