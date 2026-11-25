package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ai_career_service.dto.chat.AiQuotaResponse;
import com.example.ai_career_service.model.AiUsage;
import com.example.ai_career_service.repository.AiUsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiUsageServiceTests {

    @Test
    void consumeQuota_incrementsAndBlocksAtLimitWithResetTime() {
        AiUsageRepository repo = mock(AiUsageRepository.class);
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T07:50:30Z"), zone);
        AiUsageService service = new AiUsageService(repo, 30, zone, fixedClock);

        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now(fixedClock);

        when(repo.findByIdUserIdAndIdDate(userId, today))
                .thenReturn(Optional.of(new AiUsage(userId, today, 29)));
        when(repo.save(any(AiUsage.class))).thenAnswer(inv -> inv.getArgument(0));

        AiQuotaResponse after29 = service.consumeQuota(userId);
        assertThat(after29.used()).isEqualTo(30);
        assertThat(after29.remaining()).isEqualTo(0);
        assertThat(after29.resetAt()).startsWith("2026-11-26T00:00");

        when(repo.findByIdUserIdAndIdDate(userId, today))
                .thenReturn(Optional.of(new AiUsage(userId, today, 30)));

        assertThatThrownBy(() -> service.consumeQuota(userId))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("30")
                .satisfies(ex -> {
                    QuotaExceededException qe = (QuotaExceededException) ex;
                    assertThat(qe.getQuota().remaining()).isEqualTo(0);
                    assertThat(qe.getQuota().resetAt()).startsWith("2026-11-26T00:00");
                });
    }
}
