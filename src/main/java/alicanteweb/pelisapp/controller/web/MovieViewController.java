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
            model.addAttribute("reviews", reviews);
            model.addAttribute("movieStats", stats);
            model.addAttribute("userReview", userReview);
            model.addAttribute("canReview", isAuthenticated && userReview == null);
            model.addAttribute("isAuthenticated", isAuthenticated);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("currentUser", currentUser);
            return "movie-detail";
        } catch (Exception e) {
            log.error("Error cargando detalles de película {}: {}", id, e.getMessage());
            model.addAttribute("error", "No se pudo cargar la película");
            return "error";
        }
    }

    private MovieStats calculateMovieStats(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return new MovieStats(0, 0.0, new int[5]);
        }
        double totalRating = 0;
        int[] starDistribution = new int[5];
        for (Review review : reviews) {
            Integer stars = review.getStars();
            if (stars == null || stars < 1 || stars > 5) {
                log.warn("Reseña {} con puntuación inválida: {}", review.getId(), stars);
                continue;
            }
            totalRating += stars;
            starDistribution[stars - 1]++;
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
