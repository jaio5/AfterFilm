package alicanteweb.pelisapp.dto;

import java.time.Instant;

public record MessageDTO(
        Long id,
        String senderUsername,
        String receiverUsername,
        String content,
        Instant sentAt,
        boolean read,
        String sharedContentType,
        Long sharedContentId,
        String sharedContentTitle,
        String sharedContentPoster
) {}