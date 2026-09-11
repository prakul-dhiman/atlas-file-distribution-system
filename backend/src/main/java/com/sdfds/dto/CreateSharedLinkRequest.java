package com.sdfds.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSharedLinkRequest {

    private Long fileId;
    private Long folderId;
    private String password;

    @Min(value = 1, message = "Expiry days must be at least 1")
    @Max(value = 90, message = "Expiry days cannot exceed 90")
    private Integer expiresAfterDays;

    private Boolean neverExpire;
    private java.time.Instant customExpiresAt;
    private Boolean expireAfterFirstDownload;
    private Boolean requireEmailOtp;
    private Long maxAccessCount;
}
