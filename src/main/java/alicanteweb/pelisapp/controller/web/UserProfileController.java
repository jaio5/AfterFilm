package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.UserContentList;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.service.ImageUrlService;
import alicanteweb.pelisapp.service.UserListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final UserListService userListService;
    private final ImageUrlService imageUrlService;

    @GetMapping("/perfil")
    @Transactional(readOnly = true)
    public String perfil(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            List<Review> userReviews = reviewRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId());
            // Seguidores y seguidos
            int followersCount = user.getFollowers() != null ? user.getFollowers().size() : 0;
            int followingCount = user.getFollowing() != null ? user.getFollowing().size() : 0;
            // Logros
            var archivements = user.getUsuarioArchievements();
            // Carátulas de películas (ya accesibles desde review.getMovie().getPosterLocalPath())
            double avgRating = userReviews.stream()
                    .mapToInt(Review::getStars)
                    .average()
                    .orElse(0.0);
            List<UserContentList> favorites;
            List<UserContentList> watchlist;
            try {
                favorites = userListService.getList(username, UserListService.FAVORITE);
                watchlist = userListService.getList(username, UserListService.WATCHLIST);
            } catch (Exception e) {
                log.warn("No se pudieron cargar las listas del usuario: {}", e.getMessage());
                favorites = List.of();
                watchlist = List.of();
            }
            model.addAttribute("user", user);
            model.addAttribute("reviews", userReviews);
            model.addAttribute("reviewPosterUrls", reviewPosterUrls(userReviews));
            model.addAttribute("reviewCount", userReviews.size());
            model.addAttribute("avgRating", avgRating);
            model.addAttribute("followersCount", followersCount);
            model.addAttribute("followingCount", followingCount);
            model.addAttribute("archivements", archivements);
            model.addAttribute("favorites", favorites);
            model.addAttribute("watchlist", watchlist);
            return model.containsAttribute("user") ? "perfil" : "error";
        } catch (Exception e) {
            log.error("Error cargando perfil", e);
            model.addAttribute("error", "Error cargando perfil: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "error";
        }
    }

    private Map<Long, String> reviewPosterUrls(List<Review> reviews) {
        Map<Long, String> urls = new HashMap<>();
        for (Review review : reviews) {
            if (review.getId() == null) {
                continue;
            }
            String posterUrl = imageUrlService.reviewContentPosterUrl(review, "w500");
            if (posterUrl != null && !posterUrl.isBlank()) {
                urls.put(review.getId(), posterUrl);
            }
        }
        return urls;
    }
}
