package com.example.recruitment_service.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class OfferExpirationSchedulerTest {

    @Test
    void hourlyJobDelegatesToOfferService() {
        OfferService offerService = mock(OfferService.class);
        when(offerService.expireOverdueOffers()).thenReturn(2);

        OfferExpirationScheduler scheduler = new OfferExpirationScheduler(offerService);
        scheduler.expireOffersHourly();

        verify(offerService).expireOverdueOffers();
    }
}
