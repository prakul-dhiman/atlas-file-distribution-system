package com.sdfds.service.impl;

import com.sdfds.dto.AnalyticsOverviewDto;
import com.sdfds.entity.ShareDownloadEvent;
import com.sdfds.entity.User;
import com.sdfds.repository.ShareDownloadEventRepository;
import com.sdfds.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ShareDownloadEventRepository downloadEventRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsOverviewDto getOverview(User user) {
        long totalDownloads = downloadEventRepository.countByUser(user);
        long uniqueDownloads = downloadEventRepository.countUniqueByUser(user);
        long failedDownloads = downloadEventRepository.countByUserAndStatus(user, "FAILED");
        long suspiciousDownloads = downloadEventRepository.countByUserAndStatus(user, "SUSPICIOUS");

        List<Object[]> countryRows = downloadEventRepository.countByCountryForUser(user);
        List<AnalyticsOverviewDto.CountryStat> countries = new ArrayList<>();
        for (Object[] row : countryRows) {
            countries.add(AnalyticsOverviewDto.CountryStat.builder()
                    .countryCode((String) row[0])
                    .country((String) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }

        List<Object[]> deviceRows = downloadEventRepository.countByDeviceForUser(user);
        List<AnalyticsOverviewDto.DeviceStat> devices = new ArrayList<>();
        for (Object[] row : deviceRows) {
            devices.add(AnalyticsOverviewDto.DeviceStat.builder()
                    .device(row[0] != null ? (String) row[0] : "Desktop")
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        List<Object[]> browserRows = downloadEventRepository.countByBrowserForUser(user);
        List<AnalyticsOverviewDto.BrowserStat> browsers = new ArrayList<>();
        for (Object[] row : browserRows) {
            browsers.add(AnalyticsOverviewDto.BrowserStat.builder()
                    .browser(row[0] != null ? (String) row[0] : "Unknown")
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        List<Object[]> osRows = downloadEventRepository.countByOsForUser(user);
        List<AnalyticsOverviewDto.OsStat> osStats = new ArrayList<>();
        for (Object[] row : osRows) {
            osStats.add(AnalyticsOverviewDto.OsStat.builder()
                    .os(row[0] != null ? (String) row[0] : "Unknown")
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        List<Object[]> topFileRows = downloadEventRepository.findTopDownloadedFilesForUser(user);
        List<AnalyticsOverviewDto.TopFileStat> topFiles = new ArrayList<>();
        long totalBytes = 0;
        for (Object[] row : topFileRows) {
            long count = ((Number) row[2]).longValue();
            long bytes = row[3] != null ? ((Number) row[3]).longValue() : 0;
            totalBytes += bytes;
            topFiles.add(AnalyticsOverviewDto.TopFileStat.builder()
                    .fileId((Long) row[0])
                    .fileName((String) row[1])
                    .count(count)
                    .totalBytes(bytes)
                    .build());
        }

        List<ShareDownloadEvent> events = downloadEventRepository.findByUserOrderByDownloadedAtDesc(user);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        List<AnalyticsOverviewDto.RecentEventDto> recentEvents = new ArrayList<>();
        for (int i = 0; i < Math.min(events.size(), 20); i++) {
            ShareDownloadEvent e = events.get(i);
            recentEvents.add(AnalyticsOverviewDto.RecentEventDto.builder()
                    .id(e.getId())
                    .fileName(e.getFile() != null ? e.getFile().getName() : "Shared Item")
                    .ipAddress(e.getIpAddress())
                    .country(e.getCountry())
                    .city(e.getCity())
                    .device(e.getDevice())
                    .browser(e.getBrowser())
                    .os(e.getOperatingSystem())
                    .status(e.getStatus() != null ? e.getStatus() : "SUCCESS")
                    .downloadedAt(e.getDownloadedAt() != null ? formatter.format(e.getDownloadedAt()) : "")
                    .build());
        }

        return AnalyticsOverviewDto.builder()
                .totalDownloads(totalDownloads)
                .uniqueDownloads(uniqueDownloads)
                .failedDownloads(failedDownloads)
                .suspiciousDownloads(suspiciousDownloads)
                .totalBytesTransferred(totalBytes)
                .countries(countries)
                .devices(devices)
                .browsers(browsers)
                .operatingSystems(osStats)
                .topFiles(topFiles)
                .recentEvents(recentEvents)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportCsv(User user) {
        List<ShareDownloadEvent> events = downloadEventRepository.findByUserOrderByDownloadedAtDesc(user);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,File,IP Address,Country,City,Device,Browser,OS,Status,Downloaded At\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        for (ShareDownloadEvent e : events) {
            sb.append(e.getId()).append(",")
              .append("\"").append(e.getFile() != null ? e.getFile().getName().replace("\"", "\"\"") : "Shared Item").append("\",")
              .append(e.getIpAddress() != null ? e.getIpAddress() : "").append(",")
              .append("\"").append(e.getCountry() != null ? e.getCountry() : "").append("\",")
              .append("\"").append(e.getCity() != null ? e.getCity() : "").append("\",")
              .append(e.getDevice() != null ? e.getDevice() : "").append(",")
              .append(e.getBrowser() != null ? e.getBrowser() : "").append(",")
              .append(e.getOperatingSystem() != null ? e.getOperatingSystem() : "").append(",")
              .append(e.getStatus() != null ? e.getStatus() : "SUCCESS").append(",")
              .append(e.getDownloadedAt() != null ? formatter.format(e.getDownloadedAt()) : "").append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
