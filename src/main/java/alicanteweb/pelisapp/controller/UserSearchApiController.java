package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.UserDTO;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSearchApiController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam(name = "query", defaultValue = "") String q,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.ok(List.of());
        if (q == null || q.trim().isEmpty()) return ResponseEntity.ok(List.of());
        int safeSize = Math.max(1, Math.min(size, 20));
        String query = q.trim();
        var page = userRepository.searchByUsernameOrDisplayName(query, PageRequest.of(0, safeSize));
        List<UserDTO> results = page.stream()
                .filter(u -> !u.getUsername().equalsIgnoreCase(userDetails.getUsername()))
                .map(u -> new UserDTO(u.getId(), u.getUsername(), u.getDisplayName(), null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }
}
