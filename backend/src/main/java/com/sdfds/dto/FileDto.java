package com.sdfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {

    private Long id;
    private String name;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private String checksumSha256;
    private Long folderId;
    private Long userId;
    private Boolean isChunked;
    private Boolean isTrashed;
    private Boolean isStarred;
    private Instant trashedAt;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
}
