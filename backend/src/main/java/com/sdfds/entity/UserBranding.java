package com.sdfds.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "user_branding")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "brand_name", length = 100)
    private String brandName;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "primary_color", length = 10)
    @Builder.Default
    private String primaryColor = "#6366f1";

    @Column(name = "accent_color", length = 10)
    @Builder.Default
    private String accentColor = "#8b5cf6";

    @Column(name = "background_color", length = 10)
    @Builder.Default
    private String backgroundColor = "#0f172a";

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(name = "support_email", length = 100)
    private String supportEmail;

    @Column(name = "show_powered_by", nullable = false)
    @Builder.Default
    private Boolean showPoweredBy = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
