package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ContentReviewRequest;
import alicanteweb.pelisapp.dto.ReviewCreateRequest;
import alicanteweb.pelisapp.dto.ReviewDTO;
import alicanteweb.pelisapp.dto.ReviewReplyDTO;
import alicanteweb.pelisapp.dto.ReviewReplyRequest;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.ReviewReply;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.ReviewReplyService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final ReviewReplyService reviewReplyService;
    private final UserRepository userRepository;

    @PostMapping("")
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody ReviewCreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Review review = reviewService.createReview(
                user.getId(),
                EndpointSanitizer.id(req.getMovieId(), "movieId"),
                EndpointSanitizer.optionalText(req.getText(), 1000),
                req.getStars());
        return ResponseEntity.ok(toDto(review));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(@PathVariable("id") Long reviewId, Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        reviewService.likeReview(user.getId(), EndpointSanitizer.id(reviewId, "reviewId"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/replies")
    public ResponseEntity<List<ReviewReplyDTO>> getReplies(@PathVariable("id") Long reviewId) {
        List<ReviewReplyDTO> replies = reviewReplyService
                .getReplies(EndpointSanitizer.id(reviewId, "reviewId"))
                .stream()
                .map(ReviewApiController::toReplyDto)
                .toList();
        return ResponseEntity.ok(replies);
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<ReviewReplyDTO> createReply(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody ReviewReplyRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        ReviewReply reply = reviewReplyService.createReply(
                EndpointSanitizer.id(reviewId, "reviewId"),
                user.getId(),
                EndpointSanitizer.optionalText(req.getText(), 1000));
        return ResponseEntity.status(HttpStatus.CREATED).body(toReplyDto(reply));
    }

    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        reviewReplyService.deleteOwnReply(EndpointSanitizer.id(replyId, "replyId"), user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody ContentReviewRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Review review = reviewService.updateReview(
                user.getId(),
                EndpointSanitizer.id(reviewId, "reviewId"),
                EndpointSanitizer.optionalText(req.getText(), 1000),
                req.getStars());
        return ResponseEntity.ok(toDto(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("id") Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        reviewService.deleteOwnReview(user.getId(), EndpointSanitizer.id(reviewId, "reviewId"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/series/{seriesId}")
    public ResponseEntity<ReviewDTO> createSeriesReview(
            @PathVariable Long seriesId,
            @Valid @RequestBody ContentReviewRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Review review = reviewService.createSeriesReview(
                user.getId(),
                EndpointSanitizer.id(seriesId, "seriesId"),
                EndpointSanitizer.optionalText(req.getText(), 1000),
                req.getStars());
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
        Review review = reviewService.createBookReview(
                user.getId(),
                EndpointSanitizer.id(bookId, "bookId"),
                EndpointSanitizer.optionalText(req.getText(), 1000),
                req.getStars());
        return ResponseEntity.ok(toDto(review));
    }

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsBySeries(@PathVariable Long seriesId) {
        List<Review> reviews = reviewService.getReviewsBySeriesId(EndpointSanitizer.id(seriesId, "seriesId"));
        return ResponseEntity.ok(reviews.stream().map(ReviewApiController::toDto).toList());
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByBook(@PathVariable Long bookId) {
        List<Review> reviews = reviewService.getReviewsByBookId(EndpointSanitizer.id(bookId, "bookId"));
        return ResponseEntity.ok(reviews.stream().map(ReviewApiController::toDto).toList());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurity(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByMovie(@PathVariable Long movieId) {
        List<Review> reviews = reviewService.getReviewsByMovieId(EndpointSanitizer.id(movieId, "movieId"));
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

    private static ReviewReplyDTO toReplyDto(ReviewReply reply) {
        ReviewReplyDTO dto = new ReviewReplyDTO();
        dto.setId(reply.getId());
        dto.setReviewId(reply.getReview() != null ? reply.getReview().getId() : null);
        ReviewReplyDTO.SimpleUserDTO userDto = new ReviewReplyDTO.SimpleUserDTO();
        if (reply.getUser() != null) {
            userDto.setId(reply.getUser().getId());
            userDto.setUsername(reply.getUser().getUsername());
        }
        dto.setUser(userDto);
        dto.setText(reply.getText());
        dto.setCreatedAt(reply.getCreatedAt());
        dto.setUpdatedAt(reply.getUpdatedAt());
        return dto;
    }

}
