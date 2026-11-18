package com.example.recruitment_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OfferExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(OfferExpirationScheduler.class);

    private final OfferService offerService;

    public OfferExpirationScheduler(OfferService offerService) {
        this.offerService = offerService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void expireOffersHourly() {
        int expiredCount = offerService.expireOverdueOffers();
        if (expiredCount > 0) {
            log.info("Expired {} overdue offer(s) and marked their applications REJECTED", expiredCount);
        }
    }
}
