package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.dto.ContentStats;
import alicanteweb.pelisapp.dto.TvShowDetailDTO;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.ReviewReply;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.ReviewReplyRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.ImageUrlService;
import alicanteweb.pelisapp.service.TvShowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SeriesViewController {

    private final TvShowRepository tvShowRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final UserRepository userRepository;
    private final ImageUrlService imageUrlService;
    private final TvShowService tvShowService;

    @GetMapping("/serie/{id}")
    public String seriesDetail(@PathVariable Long id, Model model, Authentication auth) {
        try {
            TvShow series = tvShowRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Serie no encontrada"));
            TvShowDetailDTO seriesDetails = tvShowService.getSeriesById(id).orElse(null);
            List<Review> reviews = reviewRepository.findBySeriesIdOrderByCreatedAtDesc(series.getId());
            ContentStats stats = calculateStats(reviews);
            Review userReview = null;
            User currentUser = null;
            boolean isAuthenticated = auth != null
                    && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken);
            if (isAuthenticated) {
                currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
                if (currentUser != null) {
                    List<Review> currentUserReviews = reviewRepository
                            .findAllByUserIdAndSeriesIdOrderByCreatedAtDesc(currentUser.getId(), series.getId());
                    userReview = currentUserReviews.isEmpty() ? null : currentUserReviews.get(0);
                }
            }
            boolean isAdmin = isAuthenticated && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("series", series);
            model.addAttribute("seriesDetails", seriesDetails);
            model.addAttribute("posterUrl", imageUrlService.seriesPosterUrl(series, "w500"));
            model.addAttribute("reviews", reviews);
            model.addAttribute("reviewReplies", repliesByReviewId(reviews));
            model.addAttribute("stats", stats);
            model.addAttribute("userReview", userReview);
            model.addAttribute("canReview", currentUser != null && userReview == null);
            model.addAttribute("isAuthenticated", isAuthenticated);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("currentUser", currentUser);
            return "series-detail";
        } catch (Exception e) {
            log.error("Error cargando detalle de serie {}: {}", id, e.getMessage());
            model.addAttribute("error", "No se pudo cargar la serie");
            return "error";
        }
    }

    private ContentStats calculateStats(List<Review> reviews) {
        if (reviews.isEmpty()) return new ContentStats(0, 0.0, new int[5]);
        double total = 0;
        int[] dist = new int[5];
        for (Review r : reviews) {
            Double stars = r.getStars();
            if (stars == null || stars < 0.5 || stars > 5) {
                log.warn("Reseña {} con puntuacion invalida: {}", r.getId(), stars);
                continue;
            }
            total += stars;
            dist[Math.max(0, Math.min(4, (int) Math.ceil(stars) - 1))]++;
        }
        int validReviews = java.util.Arrays.stream(dist).sum();
        return new ContentStats(validReviews, validReviews > 0 ? total / validReviews : 0.0, dist);
    }

    private Map<Long, List<ReviewReply>> repliesByReviewId(List<Review> reviews) {
        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .filter(id -> id != null)
                .toList();
        Map<Long, List<ReviewReply>> replies = reviewIds.stream()
                .collect(Collectors.toMap(id -> id, id -> List.<ReviewReply>of(), (a, b) -> a, HashMap::new));
        if (reviewIds.isEmpty()) {
            return replies;
        }
        replies.putAll(reviewReplyRepository.findByReview_IdInOrderByCreatedAtAsc(reviewIds).stream()
                .collect(Collectors.groupingBy(reply -> reply.getReview().getId()));
        return replies;
    }

}
