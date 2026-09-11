package com.sdfds.mapper;

import com.sdfds.dto.UserDto;
import com.sdfds.entity.Role;
import com.sdfds.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .storageQuotaBytes(user.getStorageQuotaBytes())
                .usedStorageBytes(user.getUsedStorageBytes())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
