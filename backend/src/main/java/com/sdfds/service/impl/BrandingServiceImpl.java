package com.sdfds.service.impl;

import com.sdfds.dto.BrandingDto;
import com.sdfds.entity.User;
import com.sdfds.entity.UserBranding;
import com.sdfds.repository.UserBrandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandingServiceImpl {

    private final UserBrandingRepository brandingRepository;

    @Transactional
    public BrandingDto upsertBranding(User user, BrandingDto dto) {
        UserBranding branding = brandingRepository.findByUser(user)
                .orElse(UserBranding.builder().user(user).build());

        if (dto.getBrandName() != null) branding.setBrandName(dto.getBrandName());
        if (dto.getLogoUrl() != null) branding.setLogoUrl(dto.getLogoUrl());
        if (dto.getPrimaryColor() != null) branding.setPrimaryColor(dto.getPrimaryColor());
        if (dto.getAccentColor() != null) branding.setAccentColor(dto.getAccentColor());
        if (dto.getBackgroundColor() != null) branding.setBackgroundColor(dto.getBackgroundColor());
        if (dto.getWelcomeMessage() != null) branding.setWelcomeMessage(dto.getWelcomeMessage());
        if (dto.getSupportEmail() != null) branding.setSupportEmail(dto.getSupportEmail());
        if (dto.getShowPoweredBy() != null) branding.setShowPoweredBy(dto.getShowPoweredBy());
        branding.setUpdatedAt(Instant.now());

        UserBranding saved = brandingRepository.save(branding);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public BrandingDto getBranding(User user) {
        return brandingRepository.findByUser(user)
                .map(this::toDto)
                .orElse(defaultBranding());
    }

    @Transactional(readOnly = true)
    public BrandingDto getBrandingForShare(User shareOwner) {
        return brandingRepository.findByUser(shareOwner)
                .map(this::toDto)
                .orElse(defaultBranding());
    }

    private BrandingDto toDto(UserBranding b) {
        return BrandingDto.builder()
                .id(b.getId())
                .brandName(b.getBrandName())
                .logoUrl(b.getLogoUrl())
                .primaryColor(b.getPrimaryColor())
                .accentColor(b.getAccentColor())
                .backgroundColor(b.getBackgroundColor())
                .welcomeMessage(b.getWelcomeMessage())
                .supportEmail(b.getSupportEmail())
                .showPoweredBy(b.getShowPoweredBy())
                .build();
    }

    private BrandingDto defaultBranding() {
        return BrandingDto.builder()
                .primaryColor("#6366f1")
                .accentColor("#8b5cf6")
                .backgroundColor("#0f172a")
                .showPoweredBy(true)
                .build();
    }
}
