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
        if (!listType.equals(UserListService.FAVORITE) && !listType.equals(UserListService.WATCHLIST)) {
            return ResponseEntity.badRequest().body(Map.of("error", "listType must be FAVORITE or WATCHLIST"));
        }
        boolean active = userListService.toggle(
                userDetails.getUsername(), listType, contentType, contentId,
                title, poster.isEmpty() ? null : poster);
        return ResponseEntity.ok(Map.of("active", active, "listType", listType));
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
        return ResponseEntity.ok(Map.of(
                "favorite",  userListService.isInList(username, UserListService.FAVORITE,  contentType, contentId),
                "watchlist", userListService.isInList(username, UserListService.WATCHLIST, contentType, contentId)
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}