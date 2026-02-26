package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ReviewCreateRequest;
import alicanteweb.pelisapp.dto.ReviewDTO;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping("")
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody ReviewCreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Review review = reviewService.createReview(user.getId(), req.getMovieId(), req.getText(), req.getStars());
        return ResponseEntity.ok(toDto(review));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(@PathVariable("id") Long reviewId, Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        reviewService.likeReview(user.getId(), reviewId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByMovie(@PathVariable Long movieId) {
        List<Review> reviews = reviewService.getReviewsByMovieId(movieId);
        List<ReviewDTO> dtos = reviews.stream().map(ReviewApiController::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    private static ReviewDTO toDto(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        ReviewDTO.SimpleUserDTO userDto = new ReviewDTO.SimpleUserDTO();
        userDto.setId(review.getUser().getId());
        userDto.setUsername(review.getUser().getUsername());
        dto.setUser(userDto);
        ReviewDTO.SimpleMovieDTO movieDto = new ReviewDTO.SimpleMovieDTO();
        movieDto.setId(review.getMovie().getId());
        movieDto.setTitle(review.getMovie().getTitle());
        dto.setMovie(movieDto);
        dto.setText(review.getText());
        dto.setStars(review.getStars());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setLikesCount(review.getLikesCount());
        return dto;
    }
}
