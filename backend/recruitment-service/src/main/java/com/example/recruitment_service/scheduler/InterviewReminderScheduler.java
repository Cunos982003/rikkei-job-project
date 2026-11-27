package com.example.recruitment_service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.recruitment_service.service.InterviewScheduleService;

@Component
public class InterviewReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(InterviewReminderScheduler.class);

    private final InterviewScheduleService interviewScheduleService;

    public InterviewReminderScheduler(InterviewScheduleService interviewScheduleService) {
        this.interviewScheduleService = interviewScheduleService;
    }

    @Scheduled(cron = "${app.interview.reminder-cron:0 */15 * * * *}")
    public void runReminders() {
        var result = interviewScheduleService.sendDueReminders();
        if (result.sent24hCount() > 0 || result.sent1hCount() > 0) {
            log.info("Interview reminder job completed: sent24h={}, sent1h={}",
                    result.sent24hCount(), result.sent1hCount());
        }
    }
}
