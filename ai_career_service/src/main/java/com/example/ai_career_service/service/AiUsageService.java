package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.chat.AiQuotaResponse;
import com.example.ai_career_service.model.AiUsage;
import com.example.ai_career_service.repository.AiUsageRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageService {

    private final AiUsageRepository aiUsageRepository;
    private final int dailyLimit;
    private final ZoneId zoneId;
    private final Clock clock;

    @Autowired
    public AiUsageService(
            AiUsageRepository aiUsageRepository,
            @Value("${app.ai.daily-limit:30}") int dailyLimit,
            @Value("${app.ai.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this(aiUsageRepository, dailyLimit, ZoneId.of(timezone), Clock.system(ZoneId.of(timezone)));
    }

    AiUsageService(AiUsageRepository aiUsageRepository, int dailyLimit, ZoneId zoneId, Clock clock) {
        this.aiUsageRepository = aiUsageRepository;
        this.dailyLimit = Math.max(1, dailyLimit);
        this.zoneId = zoneId;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiQuotaResponse getQuota(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        int used = aiUsageRepository.findByIdUserIdAndIdDate(userId, today)
                .map(AiUsage::getCount)
                .orElse(0);
        return toResponse(used, today);
    }

    @Transactional
    public AiQuotaResponse consumeQuota(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        AiUsage usage = aiUsageRepository.findByIdUserIdAndIdDate(userId, today)
                .orElseGet(() -> new AiUsage(userId, today, 0));

        if (usage.getCount() >= dailyLimit) {
            AiQuotaResponse quota = toResponse(usage.getCount(), today);
            throw new QuotaExceededException(
                    "Bạn đã sử dụng hết " + dailyLimit + " lượt hỏi AI hôm nay. Vui lòng quay lại sau khi đặt lại lúc "
                            + quota.resetAt() + ".",
                    quota);
        }

        usage.setCount(usage.getCount() + 1);
        aiUsageRepository.save(usage);
        return toResponse(usage.getCount(), today);
    }

    private AiQuotaResponse toResponse(int used, LocalDate today) {
        int remaining = Math.max(0, dailyLimit - used);
        ZonedDateTime resetTime = today.plusDays(1).atStartOfDay(zoneId);
        return new AiQuotaResponse(used, dailyLimit, remaining, resetTime.toOffsetDateTime().toString());
    }
}
