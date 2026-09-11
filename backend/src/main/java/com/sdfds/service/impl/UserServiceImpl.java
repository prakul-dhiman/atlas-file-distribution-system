package com.sdfds.service.impl;

import com.sdfds.dto.UserDto;
import com.sdfds.entity.User;
import com.sdfds.mapper.UserMapper;
import com.sdfds.repository.UserRepository;
import com.sdfds.service.AuditLogService;
import com.sdfds.service.UserService;
import com.sdfds.storage.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final long MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final LocalStorageService localStorageService;
    private final AuditLogService auditLogService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.public-backend-url:http://localhost:8081}")
    private String publicBackendUrl;

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional
    public UserDto uploadProfileImage(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile image file is required");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_BYTES) {
            throw new IllegalArgumentException("Profile image must be 5 MB or smaller");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        // Validate the file extension as a second line of defense
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }
        if (!Set.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        // Use a deterministic file id so we overwrite the previous profile image
        Long profileFileId = 0L; // convention: profile image is stored under user dir with id 0
        try {
            localStorageService.saveFile(user.getId(), profileFileId, file.getInputStream());
        } catch (IOException e) {
            log.error("Failed to store profile image for user ID: {}", user.getId(), e);
            throw new RuntimeException("Failed to store profile image", e);
        }

        String imageUrl = publicBackendUrl + "/api/v1/users/" + user.getId() + "/profile-image";
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);

        auditLogService.log("PROFILE_IMAGE_UPDATED", "User updated profile image: " + user.getUsername(), user);
        log.info("Profile image updated for user ID: {}", user.getId());

        return userMapper.toDto(user);
    }

    /**
     * Loads the profile image file for a user as a resource.
     * Publicly accessible because the returned URL is a plain static URL.
     */
    @Override
    public org.springframework.core.io.Resource loadProfileImage(Long userId) {
        try {
            return localStorageService.loadFileAsResource(userId, 0L);
        } catch (IOException e) {
            throw new UsernameNotFoundException("Profile image not found for user: " + userId);
        }
    }

    @Override
    @Transactional
    public void changePassword(User user, com.sdfds.dto.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 10) {
            throw new IllegalArgumentException("New password must be at least 10 characters long");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.log("PASSWORD_CHANGED", "User changed password: " + user.getUsername(), user);
        log.info("Password changed successfully for user ID: {}", user.getId());
    }
}