package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.service.UserListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class UserListApiController {

    private final UserListService userListService;

    /**
     * Toggle an item in FAVORITE or WATCHLIST.
     * Returns {active: true/false, listType: "FAVORITE"|"WATCHLIST"}
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            @RequestParam String listType,
            @RequestParam String contentType,
            @RequestParam Long contentId,
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String poster,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        String safeListType = EndpointSanitizer.listType(listType);
        String safeContentType = EndpointSanitizer.contentType(contentType);
        Long safeContentId = EndpointSanitizer.id(contentId, "contentId");
        boolean active = userListService.toggle(
                userDetails.getUsername(), safeListType, safeContentType, safeContentId,
                EndpointSanitizer.requiredText(title, "title", 200),
                EndpointSanitizer.optionalText(poster, 1000));
        return ResponseEntity.ok(Map.of("active", active, "listType", safeListType));
    }

    /**
     * Get favorite + watchlist status for a single content item.
     * Returns {favorite: true/false, watchlist: true/false}
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @RequestParam String contentType,
            @RequestParam Long contentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("favorite", false, "watchlist", false));
        }
        String username = userDetails.getUsername();
        String safeContentType = EndpointSanitizer.contentType(contentType);
        Long safeContentId = EndpointSanitizer.id(contentId, "contentId");
        return ResponseEntity.ok(Map.of(
                "favorite",  userListService.isInList(username, UserListService.FAVORITE, safeContentType, safeContentId),
                "watchlist", userListService.isInList(username, UserListService.WATCHLIST, safeContentType, safeContentId)
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
