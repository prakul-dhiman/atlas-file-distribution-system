package com.sdfds.service;

import com.sdfds.dto.CreateDirectShareRequest;
import com.sdfds.dto.DirectShareDto;
import com.sdfds.entity.DirectItemShare.PermissionLevel;
import com.sdfds.entity.User;

import java.util.List;

public interface DirectShareService {

    DirectShareDto createDirectShare(User grantor, CreateDirectShareRequest request);

    List<DirectShareDto> getSharedWithMe(User user);

    List<DirectShareDto> getItemCollaborators(User user, Long fileId, Long folderId);

    DirectShareDto updatePermission(User user, Long shareId, PermissionLevel level);

    void revokeShare(User user, Long shareId);
}
