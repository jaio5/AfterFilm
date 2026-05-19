package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.constants.AppConstants;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.Role;
import alicanteweb.pelisapp.entity.Tag;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.RoleRepository;
import alicanteweb.pelisapp.repository.ReviewLikeRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.TagRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de usuarios y lógica de logros.
 * Implementa principios SOLID y separación de responsabilidades.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final RoleRepository roleRepository;
    private final TagRepository tagRepository;

    /**
     * Carga los datos del perfil de usuario para la vista.
     * Centraliza la lógica de negocio del perfil de usuario.
     */
    @Transactional(readOnly = true)
    public void loadUserProfileData(String username, Authentication authentication, Model model) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        List<Review> reviews = reviewRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId());
        boolean isAdmin = hasAdminRole(authentication);

        model.addAttribute("usuario", user);
        model.addAttribute("reviews", reviews);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("allRoles", roleRepository.findAll());
        model.addAttribute("allTags", tagRepository.findAll());

        log.debug("Perfil cargado para usuario: {} con {} reviews", username, reviews.size());
    }

    /**
     * Verifica si el usuario autenticado tiene rol de administrador.
     */
    private boolean hasAdminRole(Authentication authentication) {
        if (authentication == null) return false;

        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_SUPERADMIN".equals(authority));
    }

    /**
     * Called after a user posts a review. Grants achievements for number of reviews posted.
     */
    @Transactional
    public void onUserPostedReview(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        long reviewCount = reviewRepository.countByUser_Id(userId);

        // Achievement system - grant achievements based on review count
        if (reviewCount == 1) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_FIRST_REVIEW, "Primera Reseña");
        }
        if (reviewCount == 10) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_REVIEWER_10, "Crítico Novato");
        }
        if (reviewCount == 50) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_REVIEWER_50, "Crítico Experimentado");
        }
        if (reviewCount == 100) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_CRITIC_100, "Crítico Profesional");
        }

        userRepository.save(user);
        log.info("Logros de reseñas evaluados para usuario {} - Total reseñas: {}", userId, reviewCount);
    }

    /**
     * Called when one of the user's reviews receives a like.
     * Recalculates average likes per review and assigns roles based on thresholds.
     */
    @Transactional
    public void onUserReceivedLike(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        List<Long> reviewIds = reviewRepository.findAllByUser_Id(userId).stream().map(Review::getId).toList();
        if (reviewIds.isEmpty()) return;

        long totalLikes = reviewLikeRepository.countByReview_IdIn(reviewIds);
        double avgLikesPerReview = (double) totalLikes / reviewIds.size();
        int criticLevel = (int) Math.floor(avgLikesPerReview);
        user.setCriticLevel(criticLevel);

        // Achievement system - grant achievements based on likes received
        if (totalLikes == 1) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_FIRST_LIKE, "Primer Like");
        }
        if (totalLikes == 25) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_POPULAR_25, "Popular");
        }
        if (totalLikes == 100) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_INFLUENCER_100, "Influencer");
        }

        // Role assignment based on average likes
        Optional<Role> criticRole = roleRepository.findByName(AppConstants.ROLE_CRITIC);
        if (avgLikesPerReview >= AppConstants.CRITIC_ROLE_AVG_LIKES_THRESHOLD && criticRole.isPresent()) {
            user.addRole(criticRole.get());
        }

        Optional<Role> topCritic = roleRepository.findByName(AppConstants.ROLE_TOP_CRITIC);
        if (avgLikesPerReview >= AppConstants.TOP_CRITIC_ROLE_AVG_LIKES_THRESHOLD && topCritic.isPresent()) {
            user.addRole(topCritic.get());
        }

        // Check for viral review
        List<Review> userReviews = reviewRepository.findAllByUser_Id(userId);
        boolean hasViralReview = userReviews.stream()
            .anyMatch(review -> review.getLikesCount() >= AppConstants.VIRAL_REVIEW_LIKES_THRESHOLD);

        if (hasViralReview) {
            grantTagIfNotPresent(user, AppConstants.ACHIEVEMENT_VIRAL_REVIEW, "Review Viral");
        }

        userRepository.save(user);
        log.info("Logros de likes evaluados para usuario {} - Total likes: {}, Promedio: {}",
                userId, totalLikes, avgLikesPerReview);
    }

    private void grantTagIfNotPresent(User user, String code, String name) {
        Tag tag = tagRepository.findByCode(code).orElseGet(() -> {
            Tag t = new Tag();
            t.setCode(code);
            t.setName(name);
            t.setCreatedAt(Instant.now());
            return tagRepository.save(t);
        });

        // Usa helper addTag en lugar de manipular la colección expuesta
        boolean present = user.getTags().stream().anyMatch(t -> t.getCode().equals(code));
        if (!present) {
            user.addTag(tag);
        }
    }
}
