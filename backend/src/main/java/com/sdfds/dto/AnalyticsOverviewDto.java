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
public class AnalyticsOverviewDto {
    private long totalDownloads;
    private long uniqueDownloads;
    private long failedDownloads;
    private long suspiciousDownloads;
    private long totalBytesTransferred;
    private List<CountryStat> countries;
    private List<DeviceStat> devices;
    private List<BrowserStat> browsers;
    private List<OsStat> operatingSystems;
    private List<TopFileStat> topFiles;
    private List<RecentEventDto> recentEvents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryStat {
        private String countryCode;
        private String country;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStat {
        private String device;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserStat {
        private String browser;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OsStat {
        private String os;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopFileStat {
        private Long fileId;
        private String fileName;
        private long count;
        private long totalBytes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentEventDto {
        private Long id;
        private String fileName;
        private String ipAddress;
        private String country;
        private String city;
        private String device;
        private String browser;
        private String os;
        private String status;
        private String downloadedAt;
    }
}
