package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.UserPublicDTO;
import alicanteweb.pelisapp.service.SocialService;
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

    @GetMapping("/users/search")
    public ResponseEntity<List<UserPublicDTO>> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.searchUsers(q, currentUsername));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserPublicDTO> getProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.getPublicProfile(username, currentUsername));
    }

    @PostMapping("/users/{username}/follow")
    public ResponseEntity<Map<String, Object>> follow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        socialService.follow(userDetails.getUsername(), username);
        return ResponseEntity.ok(Map.of("following", true));
    }

    @PostMapping("/users/{username}/unfollow")
    public ResponseEntity<Map<String, Object>> unfollow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        socialService.unfollow(userDetails.getUsername(), username);
        return ResponseEntity.ok(Map.of("following", false));
    }

    @GetMapping("/users/{username}/followers")
    public ResponseEntity<List<UserPublicDTO>> getFollowers(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.getFollowers(username, currentUsername));
    }

    @GetMapping("/users/{username}/following")
    public ResponseEntity<List<UserPublicDTO>> getFollowing(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(socialService.getFollowing(username, currentUsername));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}