package alicanteweb.pelisapp.dto;

import java.time.Instant;

public record UserReviewDTO(
        Long id,
        String text,
        Double stars,
        Instant createdAt,
        Instant updatedAt,
        Long likesCount,
        String contentType,
        Long contentId,
        String contentTitle,
        String contentPoster
) {}
