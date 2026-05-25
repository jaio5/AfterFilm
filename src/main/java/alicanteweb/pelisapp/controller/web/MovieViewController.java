package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.dto.MovieDetailsDTO;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MovieViewController {
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MovieService movieService;

    @GetMapping("/pelicula/{id}")
    public String movieDetail(@PathVariable Long id, Model model, Authentication auth) {
        try {
            Movie movie = movieRepository.findByIdWithCastAndDirectors(id)
                    .orElseThrow(() -> new IllegalArgumentException("Película no encontrada"));
            MovieDetailsDTO movieDetails = null;
            try {
                movieDetails = movieService.getCombinedByMovieId(id);
            } catch (Exception e) {
                log.error("Error obteniendo detalles de reparto para película {}: {}", id, e.getMessage());
            }
            List<Review> reviews = reviewRepository.findByMovieIdOrderByCreatedAtDesc(movie.getId());
            MovieStats stats = calculateMovieStats(reviews);
            List<ReviewView> reviewViews = reviews.stream().map(this::toReviewView).toList();
            Review userReview = null;
            User currentUser = null;
            boolean isAuthenticated = auth != null && auth.isAuthenticated();
            if (isAuthenticated) {
                currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
                if (currentUser != null) {
                    userReview = reviewRepository.findByUserIdAndMovieId(currentUser.getId(), movie.getId()).orElse(null);
                }
            }
            boolean isAdmin = isAuthenticated && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("movie", movie);
            model.addAttribute("movieDetails", movieDetails);
            model.addAttribute("reviews", reviewViews);
            model.addAttribute("movieStats", stats);
            ReviewView userReviewView = userReview != null ? toReviewView(userReview) : null;
            model.addAttribute("userReview", userReviewView);
            model.addAttribute("userReviewStars", userReviewView != null ? starsText(userReviewView.stars()) : "");
            model.addAttribute("userReviewText", userReviewView != null ? userReviewView.text() : "");
            model.addAttribute("userReviewHasText", userReviewView != null && userReviewView.hasText());
            model.addAttribute("canReview", isAuthenticated && userReview == null);
            model.addAttribute("isAuthenticated", isAuthenticated);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("currentUser", currentUser);
            return "movie-detail";
        } catch (Exception e) {
            log.error("Error cargando detalles de película {}", id, e);
            model.addAttribute("error", "No se pudo cargar la película");
            return "error";
        }
    }


    private String starsText(Double stars) {
        if (stars == null || stars < 0.5) {
            return "";
        }
        int fullStars = Math.min((int) Math.floor(stars), 5);
        return "★".repeat(fullStars) + (Math.abs(stars - Math.floor(stars) - 0.5) < 0.001 ? "½" : "");
    }

    private ReviewView toReviewView(Review review) {
        String username = "Usuario eliminado";
        boolean hasUser = false;
        if (review.getUser() != null
                && review.getUser().getUsername() != null
                && !review.getUser().getUsername().isBlank()) {
            username = review.getUser().getUsername();
            hasUser = true;
        }
        String initial = username.isBlank() ? "U" : username.substring(0, 1).toUpperCase();
        String text = review.getText() == null ? "" : review.getText();
        Long likesCount = review.getLikesCount() == null ? 0L : review.getLikesCount();
        String createdDate = review.getCreatedAt() == null ? "" : review.getCreatedAt().toString().substring(0, 10);
        return new ReviewView(review.getId(), username, initial, hasUser, text, review.getStars(), likesCount, createdDate);
    }

    public record ReviewView(
            Long id,
            String username,
            String userInitial,
            boolean hasUser,
            String text,
            Double stars,
            Long likesCount,
            String createdDate) {
        public boolean hasText() {
            return text != null && !text.trim().isEmpty();
        }

        public int getFullStars() {
            return stars == null ? 0 : (int) Math.floor(stars);
        }

        public boolean isHalfStar() {
            if (stars == null) {
                return false;
            }
            return Math.abs(stars - Math.floor(stars) - 0.5) < 0.001;
        }

        public int getEmptyStars() {
            int used = getFullStars() + (isHalfStar() ? 1 : 0);
            return Math.max(0, 5 - used);
        }

        public String getStarsFormatted() {
            if (stars == null) {
                return "";
            }
            return stars % 1 == 0 ? String.valueOf(stars.intValue()) : String.format("%.1f", stars);
        }
    }

    private MovieStats calculateMovieStats(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return new MovieStats(0, 0.0, new int[5]);
        }
        double totalRating = 0;
        int[] starDistribution = new int[5];
        for (Review review : reviews) {
            Double stars = review.getStars();
            if (stars == null || stars < 0.5 || stars > 5) {
                log.warn("Reseña {} con puntuación inválida: {}", review.getId(), stars);
                continue;
            }
            totalRating += stars;
            starDistribution[Math.max(0, Math.min(4, (int) Math.ceil(stars) - 1))]++;
        }
        int validReviews = java.util.Arrays.stream(starDistribution).sum();
        double averageRating = validReviews > 0 ? totalRating / validReviews : 0.0;
        return new MovieStats(validReviews, averageRating, starDistribution);
    }

    public record MovieStats(int totalReviews, double averageRating, int[] starDistribution) {
        public String getAverageRatingFormatted() {
            return totalReviews > 0 ? String.format("%.1f", averageRating) : "-";
        }
        public int getStarPercentage(int star) {
            if (totalReviews == 0) return 0;
            int count = getStarCount(star);
            return (int) Math.round(count * 100.0 / totalReviews);
        }
        public int getStarCount(int star) {
            return (star >= 1 && star <= 5) ? starDistribution[star - 1] : 0;
        }
    }
}
