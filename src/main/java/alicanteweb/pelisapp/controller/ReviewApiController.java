package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ContentReviewRequest;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @PostMapping("/series/{seriesId}")
    public ResponseEntity<ReviewDTO> createSeriesReview(
            @PathVariable Long seriesId,
            @Valid @RequestBody ContentReviewRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Review review = reviewService.createSeriesReview(user.getId(), seriesId, req.getText(), req.getStars());
        return ResponseEntity.ok(toDto(review));
    }

    @PostMapping("/books/{bookId}")
    public ResponseEntity<ReviewDTO> createBookReview(
            @PathVariable Long bookId,
            @Valid @RequestBody ContentReviewRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Review review = reviewService.createBookReview(user.getId(), bookId, req.getText(), req.getStars());
        return ResponseEntity.ok(toDto(review));
    }

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsBySeries(@PathVariable Long seriesId) {
        List<Review> reviews = reviewService.getReviewsBySeriesId(seriesId);
        return ResponseEntity.ok(reviews.stream().map(ReviewApiController::toDto).toList());
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByBook(@PathVariable Long bookId) {
        List<Review> reviews = reviewService.getReviewsByBookId(bookId);
        return ResponseEntity.ok(reviews.stream().map(ReviewApiController::toDto).toList());
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
        if (review.getMovie() != null) {
            ReviewDTO.SimpleMovieDTO movieDto = new ReviewDTO.SimpleMovieDTO();
            movieDto.setId(review.getMovie().getId());
            movieDto.setTitle(review.getMovie().getTitle());
            dto.setMovie(movieDto);
        }
        dto.setText(review.getText());
        dto.setStars(review.getStars());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setLikesCount(review.getLikesCount());
        return dto;
    }

}
