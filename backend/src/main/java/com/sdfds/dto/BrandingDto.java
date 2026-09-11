package com.sdfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingDto {
    private Long id;
    private String brandName;
    private String logoUrl;
    private String primaryColor;
    private String accentColor;
    private String backgroundColor;
    private String welcomeMessage;
    private String supportEmail;
    private Boolean showPoweredBy;
}
