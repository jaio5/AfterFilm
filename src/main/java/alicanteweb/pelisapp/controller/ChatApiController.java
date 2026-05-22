package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ChatConversationDTO;
import alicanteweb.pelisapp.dto.MessageDTO;
import alicanteweb.pelisapp.dto.SendMessageRequest;
import alicanteweb.pelisapp.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    /** List of users the authenticated user has chatted with */
    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationDTO>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(chatService.getChatPartners(userDetails.getUsername()));
    }

    /** Full conversation with a specific user (marks messages as read) */
    @GetMapping("/messages/{username}")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(chatService.getConversation(userDetails.getUsername(), EndpointSanitizer.username(username)));
    }

    /** Send a message (optionally attaching a content recommendation) */
    @PostMapping("/messages/{username}")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable String username,
            @Valid @RequestBody SendMessageRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        sanitizeMessageRequest(req);
        return ResponseEntity.ok(chatService.sendMessage(userDetails.getUsername(), EndpointSanitizer.username(username), req));
    }

    /** Unread message count for badge */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.ok(Map.of("count", 0L));
        return ResponseEntity.ok(Map.of("count", chatService.getUnreadCount(userDetails.getUsername())));
    }

    @PostMapping("/conversations/{username}/star")
    public ResponseEntity<ChatConversationDTO> starConversation(
            @PathVariable String username,
            @RequestParam(defaultValue = "true") boolean starred,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(chatService.setStarred(userDetails.getUsername(), EndpointSanitizer.username(username), starred));
    }

    @DeleteMapping("/conversations/{username}")
    public ResponseEntity<Map<String, Boolean>> deleteConversation(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        chatService.deleteConversation(userDetails.getUsername(), EndpointSanitizer.username(username));
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void sanitizeMessageRequest(SendMessageRequest req) {
        req.setContent(EndpointSanitizer.requiredText(req.getContent(), "content", 2000));
        req.setSharedContentType(req.getSharedContentType() == null
                ? null
                : EndpointSanitizer.contentType(req.getSharedContentType()));
        if (req.getSharedContentId() != null) {
            req.setSharedContentId(EndpointSanitizer.id(req.getSharedContentId(), "sharedContentId"));
        }
        req.setSharedContentTitle(EndpointSanitizer.optionalText(req.getSharedContentTitle(), 200));
        req.setSharedContentPoster(EndpointSanitizer.optionalText(req.getSharedContentPoster(), 1000));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
