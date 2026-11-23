package com.example.profileservice.dto;

import com.example.profileservice.entity.ProfileReview;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistoryItemDto {
    private UUID id;
    private ProfileReview.Action action;
    private String reason;
    private UUID actorId;
    private OffsetDateTime createdAt;

    public static ReviewHistoryItemDto from(ProfileReview review) {
        return ReviewHistoryItemDto.builder()
                .id(review.getId())
                .action(review.getAction())
                .reason(review.getReason())
                .actorId(review.getActorId())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
