package com.sdfds.service;

import com.sdfds.dto.UserDto;
import com.sdfds.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserDto getCurrentUserProfile(String usernameOrEmail);

    User getUserEntity(Long userId);

    UserDto uploadProfileImage(User user, MultipartFile file);

    Resource loadProfileImage(Long userId);

    void changePassword(User user, com.sdfds.dto.ChangePasswordRequest request);
}