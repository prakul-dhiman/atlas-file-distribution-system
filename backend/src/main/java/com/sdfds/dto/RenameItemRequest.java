package com.sdfds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenameItemRequest {

    @NotBlank(message = "New name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String newName;
}
