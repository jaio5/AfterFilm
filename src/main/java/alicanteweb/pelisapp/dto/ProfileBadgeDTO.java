package alicanteweb.pelisapp.dto;

import java.time.Instant;

public record ProfileBadgeDTO(
        Long id,
        String code,
        String name,
        String description,
        String iconUrl,
        Instant awardedAt,
        Boolean pinnedToProfile
) {}
