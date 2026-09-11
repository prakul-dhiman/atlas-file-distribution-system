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
public class FolderDto {

    private Long id;
    private String name;
    private Long parentId;
    private Long userId;
    private Boolean isTrashed;
    private Instant trashedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
