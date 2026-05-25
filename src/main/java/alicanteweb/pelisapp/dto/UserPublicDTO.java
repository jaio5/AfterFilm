package alicanteweb.pelisapp.dto;

public record UserPublicDTO(
        Long id,
        String username,
        String displayName,
        String profileImageUrl,
        long followersCount,
        long followingCount,
        long reviewCount,
        boolean following
) {}
