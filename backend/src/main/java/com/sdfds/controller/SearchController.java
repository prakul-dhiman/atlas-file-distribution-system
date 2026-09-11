package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.SearchService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search files and folders by name")
@SecurityRequirement(name = "Bearer Authentication")
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Search files and folders", description = "Searches non-trashed files and folders by name substring match (case-insensitive)")
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String q
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());

        List<FolderDto> folders = searchService.searchFolders(user, q);
        List<FileDto> files = searchService.searchFiles(user, q);

        SearchResultResponse result = SearchResultResponse.builder()
                .folders(folders)
                .files(files)
                .query(q)
                .totalResults(folders.size() + files.size())
                .build();

        return ResponseEntity.ok(ApiResponse.success(result, "Search completed"));
    }
}
