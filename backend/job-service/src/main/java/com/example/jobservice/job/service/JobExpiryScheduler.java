package com.example.jobservice.job.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "job.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class JobExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobExpiryScheduler.class);

    private final JobService jobService;

    public JobExpiryScheduler(JobService jobService) {
        this.jobService = jobService;
    }

    @Scheduled(cron = "${job.expiry.cron:0 5 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void closeExpiredJobs() {
        log.info("Bắt đầu kiểm tra và đóng tin tuyển dụng quá hạn...");
        try {
            int count = jobService.closeExpiredJobs();
            log.info("Hoàn tất: đã đóng {} tin tuyển dụng quá hạn", count);
        } catch (Exception e) {
            log.error("Lỗi khi đóng tin tuyển dụng quá hạn: {}", e.getMessage(), e);
        }
    }
}

