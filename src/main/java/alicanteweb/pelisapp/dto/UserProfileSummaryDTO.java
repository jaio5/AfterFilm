package alicanteweb.pelisapp.dto;

import java.util.List;

public record UserProfileSummaryDTO(
        Long id,
        String username,
        String displayName,
        String profileImageUrl,
        Integer criticLevel,
        long followersCount,
        long followingCount,
        long reviewCount,
        double averageRating,
        long favoritesCount,
        long watchlistCount,
        boolean following,
        List<String> roles,
        List<ProfileBadgeDTO> tags,
        List<ProfileBadgeDTO> achievements
) {}
