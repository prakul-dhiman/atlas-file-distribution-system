package com.sdfds.service;

import com.sdfds.dto.AnalyticsOverviewDto;
import com.sdfds.entity.User;

public interface AnalyticsService {
    AnalyticsOverviewDto getOverview(User user);
    byte[] exportReportCsv(User user);
}
