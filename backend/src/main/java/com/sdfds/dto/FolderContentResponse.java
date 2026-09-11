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
public class FolderContentResponse {

    private FolderDto currentFolder;
    private List<FolderDto> subfolders;
    private List<FileDto> files;
}
