package com.example.profileservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final DataSyncService dataSyncService;

    @Value("${app.sync.nightly-enabled:true}")
    private boolean nightlyEnabled;

    /**
     * Chạy định kỳ vào 01:00 AM hàng ngày cho toàn bộ ứng viên
     */
    @Scheduled(cron = "${app.sync.nightly-cron:0 0 1 * * ?}")
    public void runNightlySync() {
        if (!nightlyEnabled) {
            log.info("Nightly external records sync is disabled via configuration.");
            return;
        }
        log.info("Starting scheduled nightly sync job at 01:00 AM...");
        try {
            dataSyncService.syncAllCandidates("NIGHTLY");
            log.info("Scheduled nightly sync job completed successfully.");
        } catch (Exception e) {
            log.error("Scheduled nightly sync job encountered an error: {}", e.getMessage(), e);
        }
    }
}
