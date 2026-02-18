package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ReviewCreateRequest;
import alicanteweb.pelisapp.dto.ReviewDTO;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {
    private final ReviewService reviewService;

    @PostMapping("")
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody ReviewCreateRequest req) {
        Review review = reviewService.createReview(req.getUserId(), req.getMovieId(), req.getText(), req.getStars());
        ReviewDTO dto = ReviewApiController.toDto(review);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(@PathVariable("id") Long reviewId, @RequestParam("userId") Long userId) {
        reviewService.likeReview(userId, reviewId);
        return ResponseEntity.ok().build();
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
