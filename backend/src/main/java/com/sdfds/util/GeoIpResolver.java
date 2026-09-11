package com.sdfds.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class GeoIpResolver {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoInfo {
        private String country;
        private String countryCode;
        private String city;
    }

    public GeoInfo resolve(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank() ||
                "127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress) ||
                ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.")) {
            return GeoInfo.builder()
                    .country("Local Network")
                    .countryCode("LOCAL")
                    .city("Local Host")
                    .build();
        }

        // Return a mock / fallback location or default
        return GeoInfo.builder()
                .country("United States")
                .countryCode("US")
                .city("San Francisco")
                .build();
    }
}
