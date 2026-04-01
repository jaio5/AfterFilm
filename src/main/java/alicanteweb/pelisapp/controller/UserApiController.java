package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.UserDTO;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.service.AuthService;
import alicanteweb.pelisapp.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserApiController {
    private final AuthService authService;
    private final ReviewService reviewService;

    // Obtener los datos del usuario autenticado
    @GetMapping("")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        UserDTO user = authService.getUserDTOByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    // Obtener las reviews del usuario autenticado (paginado)
    @GetMapping("/reviews")
    public ResponseEntity<Page<Review>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<Review> reviews = reviewService.getReviewsByUsername(userDetails.getUsername(), PageRequest.of(page, size));
        return ResponseEntity.ok(reviews);
    }
}
