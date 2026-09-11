package com.sdfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {

    private List<FolderDto> folders;
    private List<FileDto> files;
    private String query;
    private int totalResults;
}
