package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ProfileImageResponse;
import alicanteweb.pelisapp.dto.UserDTO;
import alicanteweb.pelisapp.dto.UserProfileSummaryDTO;
import alicanteweb.pelisapp.dto.UserReviewDTO;
import alicanteweb.pelisapp.service.AuthService;
import alicanteweb.pelisapp.service.ReviewService;
import alicanteweb.pelisapp.service.SocialService;
import alicanteweb.pelisapp.service.UserProfileImageService;
import alicanteweb.pelisapp.service.UserProfileDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserApiController {
    private final AuthService authService;
    private final ReviewService reviewService;
    private final SocialService socialService;
    private final UserProfileImageService userProfileImageService;
    private final UserProfileDtoMapper userProfileDtoMapper;

    // Obtener los datos del usuario autenticado
    @GetMapping("")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        UserDTO user = authService.getUserDTOByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileSummaryDTO> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(socialService.getProfileSummary(userDetails.getUsername(), userDetails.getUsername()));
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String imageUrl = userProfileImageService.updateProfileImage(userDetails.getUsername(), file);
        return ResponseEntity.ok(new ProfileImageResponse(imageUrl));
    }

    // Obtener las reviews del usuario autenticado (paginado)
    @GetMapping("/reviews")
    public ResponseEntity<Page<UserReviewDTO>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<UserReviewDTO> reviews = reviewService.getReviewsByUsername(
                userDetails.getUsername(),
                PageRequest.of(EndpointSanitizer.page(page), EndpointSanitizer.size(size, 10, 50)))
                .map(userProfileDtoMapper::toReviewDto);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/comments")
    public ResponseEntity<Page<UserReviewDTO>> getMyComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return getMyReviews(userDetails, page, size);
    }
}
