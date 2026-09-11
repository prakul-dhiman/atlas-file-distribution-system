package com.sdfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Long storageQuotaBytes;
    private Long usedStorageBytes;
    private String profileImageUrl;
    private Set<String> roles;
    private Instant createdAt;
}
