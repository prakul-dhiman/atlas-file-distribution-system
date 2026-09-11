package com.sdfds.scheduler;

import com.sdfds.entity.SharedLink;
import com.sdfds.repository.SharedLinkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ShareExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ShareExpiryScheduler.class);
    private final SharedLinkRepository sharedLinkRepository;

    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    @Transactional
    public void sweepExpiredShareLinks() {
        Instant now = Instant.now();

        // 1. Disable links past expiration timestamp
        List<SharedLink> timeExpired = sharedLinkRepository.findByIsActiveTrueAndExpiresAtBefore(now);
        for (SharedLink link : timeExpired) {
            link.setIsActive(false);
            sharedLinkRepository.save(link);
            log.info("Auto-disabled time-expired share link ID: {} token: {}", link.getId(), link.getToken());
        }

        // 2. Disable links that reached max access count limit
        List<SharedLink> maxAccessLinks = sharedLinkRepository.findByIsActiveTrueAndMaxAccessCountIsNotNull();
        for (SharedLink link : maxAccessLinks) {
            if (link.getAccessCount() >= link.getMaxAccessCount()) {
                link.setIsActive(false);
                sharedLinkRepository.save(link);
                log.info("Auto-disabled limit-reached share link ID: {} token: {}", link.getId(), link.getToken());
            }
        }
    }
}
