package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.UserContentListDTO;
import alicanteweb.pelisapp.dto.UserPublicDTO;
import alicanteweb.pelisapp.dto.UserReviewDTO;
import alicanteweb.pelisapp.service.SocialService;
import alicanteweb.pelisapp.service.UserListService;
import alicanteweb.pelisapp.service.UserProfileDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialApiController {

    private final SocialService socialService;
    private final UserListService userListService;
    private final UserProfileDtoMapper userProfileDtoMapper;

    @GetMapping("/users/search")
    public ResponseEntity<List<UserPublicDTO>> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.searchUsers(
                EndpointSanitizer.requiredText(q, "q", 50), currentUsername));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserPublicDTO> getProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.getPublicProfile(EndpointSanitizer.username(username), currentUsername));
    }

    @GetMapping({"/users/{username}/reviews", "/users/{username}/comments"})
    public ResponseEntity<List<UserReviewDTO>> getUserReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String safeUsername = EndpointSanitizer.username(username);
        return ResponseEntity.ok(socialService.getUserReviews(
                        safeUsername,
                        EndpointSanitizer.page(page),
                        EndpointSanitizer.size(size, 20, 50))
                .stream()
                .map(userProfileDtoMapper::toReviewDto)
                .toList());
    }

    @GetMapping("/users/{username}/lists/{listType}")
    public ResponseEntity<List<UserContentListDTO>> getUserList(
            @PathVariable String username,
            @PathVariable String listType) {
        String safeUsername = EndpointSanitizer.username(username);
        String safeListType = EndpointSanitizer.listType(listType);
        return ResponseEntity.ok(userListService.getList(safeUsername, safeListType).stream()
                .map(UserListApiController::toDto)
                .toList());
    }

    @GetMapping("/users/{username}/favorites")
    public ResponseEntity<List<UserContentListDTO>> getUserFavorites(@PathVariable String username) {
        String safeUsername = EndpointSanitizer.username(username);
        return ResponseEntity.ok(userListService.getList(safeUsername, UserListService.FAVORITE).stream()
                .map(UserListApiController::toDto)
                .toList());
    }

    @GetMapping("/users/{username}/watchlist")
    public ResponseEntity<List<UserContentListDTO>> getUserWatchlist(@PathVariable String username) {
        String safeUsername = EndpointSanitizer.username(username);
        return ResponseEntity.ok(userListService.getList(safeUsername, UserListService.WATCHLIST).stream()
                .map(UserListApiController::toDto)
                .toList());
    }

    @PostMapping("/users/{username}/follow")
    public ResponseEntity<Map<String, Object>> follow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        socialService.follow(userDetails.getUsername(), EndpointSanitizer.username(username));
        return ResponseEntity.ok(Map.of("following", true));
    }

    @PostMapping("/users/{username}/unfollow")
    public ResponseEntity<Map<String, Object>> unfollow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        socialService.unfollow(userDetails.getUsername(), EndpointSanitizer.username(username));
        return ResponseEntity.ok(Map.of("following", false));
    }

    @GetMapping("/users/{username}/followers")
    public ResponseEntity<List<UserPublicDTO>> getFollowers(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.getFollowers(EndpointSanitizer.username(username), currentUsername));
    }

    @GetMapping("/users/{username}/following")
    public ResponseEntity<List<UserPublicDTO>> getFollowing(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.getFollowing(EndpointSanitizer.username(username), currentUsername));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
