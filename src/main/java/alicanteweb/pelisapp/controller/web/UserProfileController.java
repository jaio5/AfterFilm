package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping("/perfil")
    public String perfil(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            List<Review> userReviews = reviewRepository.findAllByUser_Id(user.getId());
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
            model.addAttribute("user", user);
            model.addAttribute("reviews", userReviews);
            model.addAttribute("reviewCount", userReviews.size());
            model.addAttribute("avgRating", avgRating);
            model.addAttribute("followersCount", followersCount);
            model.addAttribute("followingCount", followingCount);
            model.addAttribute("archivements", archivements);
            return model.containsAttribute("user") ? "perfil" : "error";
        } catch (Exception e) {
            log.error("Error cargando perfil: {}", e.getMessage());
            model.addAttribute("error", "Error cargando perfil");
            return "error";
        }
    }
}
