package alicanteweb.pelisapp.dto;

import java.time.Instant;

public record ChatConversationDTO(
        String username,
        String displayName,
        boolean starred,
        long unreadCount,
        String lastMessage,
        Instant lastSentAt
) {}
