package alicanteweb.pelisapp.dto;

import java.time.Instant;

public record UserContentListDTO(
        Long id,
        String listType,
        String contentType,
        Long contentId,
        String contentTitle,
        String contentPoster,
        Instant addedAt
) {}
