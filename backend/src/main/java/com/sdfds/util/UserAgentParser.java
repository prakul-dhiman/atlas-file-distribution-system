package com.sdfds.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAgentInfo {
        private String browser;
        private String operatingSystem;
        private String device;
    }

    public UserAgentInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UserAgentInfo.builder()
                    .browser("Unknown")
                    .operatingSystem("Unknown")
                    .device("Desktop")
                    .build();
        }

        String ua = userAgent.toLowerCase();

        // 1. Device detection
        String device = "Desktop";
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipod")) {
            device = "Mobile";
        } else if (ua.contains("ipad") || ua.contains("tablet")) {
            device = "Tablet";
        }

        // 2. OS detection
        String os = "Unknown OS";
        if (ua.contains("windows")) {
            os = "Windows";
        } else if (ua.contains("mac os") || ua.contains("macintosh")) {
            os = ua.contains("iphone") || ua.contains("ipad") ? "iOS" : "macOS";
        } else if (ua.contains("android")) {
            os = "Android";
        } else if (ua.contains("linux")) {
            os = "Linux";
        } else if (ua.contains("cros")) {
            os = "ChromeOS";
        }

        // 3. Browser detection
        String browser = "Unknown Browser";
        if (ua.contains("edg/") || ua.contains("edge/")) {
            browser = "Edge";
        } else if (ua.contains("chrome/") && !ua.contains("chromium/")) {
            browser = "Chrome";
        } else if (ua.contains("firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome/")) {
            browser = "Safari";
        } else if (ua.contains("opr/") || ua.contains("opera/")) {
            browser = "Opera";
        }

        return UserAgentInfo.builder()
                .browser(browser)
                .operatingSystem(os)
                .device(device)
                .build();
    }
}
