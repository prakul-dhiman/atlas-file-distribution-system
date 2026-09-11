package com.sdfds.scheduler;

import com.sdfds.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduler that auto-purges items in the recycle bin older than 30 days.
 * Runs at midnight every day.
 */
@Component
@RequiredArgsConstructor
public class RecycleBinCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinCleanupScheduler.class);

    private final RecycleBinService recycleBinService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledCleanup() {
        log.info("Starting scheduled recycle bin cleanup...");
        try {
            recycleBinService.autoCleanup();
            log.info("Scheduled recycle bin cleanup completed successfully");
        } catch (Exception e) {
            log.error("Scheduled recycle bin cleanup failed", e);
        }
    }
}
