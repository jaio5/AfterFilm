package alicanteweb.pelisapp.dto;

public record UserPublicDTO(
        Long id,
        String username,
        String displayName,
        long followersCount,
        long followingCount,
        long reviewCount,
        boolean following
) {}