package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.ConnectionStatus;
import alicanteweb.pelisapp.entity.*;
import alicanteweb.pelisapp.repository.*;
import alicanteweb.pelisapp.service.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador unificado para toda la API REST de administración
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'ROLE_ADMIN', 'Administrador')")
public class AdminApiController {

    // Services - Solo los esenciales
    private final TMDBMovieLoaderService tmdbMovieLoaderService;
    private final ModerationService moderationService;
    private final SystemHealthService systemHealthService;
    private final AuthService authService; // Agregado para búsqueda de usuarios
    private final MoviePosterRedownloadService moviePosterRedownloadService;
    private final MovieImportService movieImportService;

    // Repositories
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final CommentModerationRepository commentModerationRepository;
    private final ReviewRepository reviewRepository;

    // ============= USER MANAGEMENT =============

    @PostMapping("/users/{userId}/confirm-email")
    public ResponseEntity<String> confirmUserEmail(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setEmailConfirmed(true);
            userRepository.save(user);

            log.info("Email confirmado manualmente para usuario ID: {}", userId);
            return ResponseEntity.ok("Email confirmado exitosamente");

        } catch (Exception e) {
            log.error("Error confirmando email para usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<String> banUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setBanned(true);
            userRepository.save(user);

            log.info("Usuario ID {} baneado", userId);
            return ResponseEntity.ok("Usuario baneado exitosamente");

        } catch (Exception e) {
            log.error("Error baneando usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setBanned(false);
            userRepository.save(user);

            log.info("Usuario ID {} desbaneado", userId);
            return ResponseEntity.ok("Usuario desbaneado exitosamente");

        } catch (Exception e) {
            log.error("Error desbaneando usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/delete")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();

            // Verificar que no sea un superadmin
            boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_SUPERADMIN".equals(role.getName()));

            if (isSuperAdmin) {
                return ResponseEntity.badRequest().body("No se puede eliminar un superadmin");
            }

            userRepository.delete(user);

            log.info("Usuario ID {} eliminado permanentemente", userId);
            return ResponseEntity.ok("Usuario eliminado exitosamente");

        } catch (Exception e) {
            log.error("Error eliminando usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Buscar usuario por email (admin).
     */
    @GetMapping("/users/search/email")
    public ResponseEntity<List<User>> searchUsersByEmail(@RequestParam String value) {
        try {
            List<User> users = userRepository.findByEmailContainingIgnoreCase(value, PageRequest.of(0, 10)).getContent();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error buscando usuarios por email: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Buscar usuario por nombre de usuario (admin).
     */
    @GetMapping("/users/search/username")
    public ResponseEntity<List<User>> searchUsersByUsername(@RequestParam String value) {
        try {
            List<User> users = userRepository.findByUsernameContainingIgnoreCase(value, PageRequest.of(0, 10)).getContent();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error buscando usuarios por username: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Buscar usuario por email (admin).
     */
    @GetMapping("/users/by-email")
    public ResponseEntity<User> findUserByEmail(@RequestParam String email) {
        User user = authService.findUserByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Buscar usuario por nombre de usuario (admin).
     */
    @GetMapping("/users/by-username")
    public ResponseEntity<User> findUserByUsername(@RequestParam String username) {
        User user = authService.findUserByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }


    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size);
        var userPage = userRepository.findAll(pageable);
        // Evitar exponer datos sensibles, mapear a DTO básico
        var users = userPage.getContent().stream().map(user -> Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(role -> role.getName()).toArray(),
                "banned", user.isBanned(),
                "emailConfirmed", user.isEmailConfirmed()
        )).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("totalElements", userPage.getTotalElements());
        result.put("totalPages", userPage.getTotalPages());
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    // ============= TMDB INTEGRATION =============

    @PostMapping("/tmdb/load-movie/{tmdbId}")
    public ResponseEntity<Map<String, Object>> loadMovieFromTMDB(@PathVariable Long tmdbId) {
        try {
            Movie movie = tmdbMovieLoaderService.loadMovieByTmdbId(tmdbId);
            Map<String, Object> result = new HashMap<>();
            if (movie != null) {
                result.put("success", true);
                result.put("message", "Película descargada y guardada correctamente");
                result.put("movieId", movie.getId());
                result.put("title", movie.getTitle());
            } else {
                result.put("success", false);
                result.put("message", "No se pudo descargar la película (puede que ya exista o haya error de conexión)");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error cargando película {}: {}", tmdbId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/tmdb/bulk-load")
    public ResponseEntity<Map<String, Object>> bulkLoadMovies(
            @RequestParam(defaultValue = "1") int page
    ) {
        try {
            // Usar método existente
            tmdbMovieLoaderService.loadPopularMovies(Math.min(page, 5));

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Carga masiva iniciada");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error en carga masiva: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/images/reload")
    public ResponseEntity<Map<String, Object>> reloadAllMoviePosters() {
        try {
            List<Movie> movies = movieRepository.findAll();
            int reloaded = 0;
            int errors = 0;

            for (Movie movie : movies) {
                try {
                    if (movie.getTmdbId() != null && (movie.getPosterLocalPath() == null || movie.getPosterLocalPath().isBlank())) {
                        boolean ok = moviePosterRedownloadService.redownloadMoviePoster(movie);
                        if (ok) {
                            reloaded++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error recargando poster para película {}: {}", movie.getId(), e.getMessage());
                    errors++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("total_movies", movies.size());
            result.put("reloaded", reloaded);
            result.put("errors", errors);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error recargando posters: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============= CAST MANAGEMENT =============

    @GetMapping("/cast/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> getMovieCast(@PathVariable Long movieId) {
        try {
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("movieId", movieId);
            result.put("movieTitle", movie.getTitle());
            result.put("actors", movie.getActors());
            result.put("directors", movie.getDirectors());
            result.put("actorsCount", movie.getActors().size());
            result.put("directorsCount", movie.getDirectors().size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error obteniendo reparto: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/cast/movie/{movieId}/reload")
    public ResponseEntity<Map<String, Object>> reloadMovieCast(@PathVariable Long movieId) {
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie == null) return ResponseEntity.notFound().build();
        if (movie.getTmdbId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false, "message", "La película no tiene TMDB ID"));
        }
        try {
            movieImportService.importOrUpdateByTmdb(movie.getTmdbId());
            return ResponseEntity.ok(Map.of("success", true,
                "message", "Reparto actualizado desde TMDB", "movieId", movieId));
        } catch (Exception e) {
            log.error("Error recargando reparto para película {}: {}", movieId, e.getMessage());
            return ResponseEntity.ok(Map.of("success", false,
                "message", "Error: " + e.getMessage()));
        }
    }

    // ============= MODERATION =============

    @GetMapping("/moderation/stats")
    public ResponseEntity<Map<String, Object>> getModerationStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_moderations", commentModerationRepository.count());
        stats.put("pending", commentModerationRepository.countByStatus(CommentModeration.ModerationStatus.PENDING));
        stats.put("approved", commentModerationRepository.countByStatus(CommentModeration.ModerationStatus.APPROVED));
        stats.put("rejected", commentModerationRepository.countByStatus(CommentModeration.ModerationStatus.REJECTED));
        stats.put("manual_review", commentModerationRepository.countByStatus(CommentModeration.ModerationStatus.MANUAL_REVIEW));
        stats.put("ollama_available", moderationService.isOllamaAvailable());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/moderation/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingModerations() {
        List<CommentModeration> pending = commentModerationRepository
                .findByStatusOrderByCreatedAsc(CommentModeration.ModerationStatus.PENDING);
        List<Map<String, Object>> result = pending.stream().map(m -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", m.getId());
            dto.put("status", m.getStatus());
            dto.put("toxicityScore", m.getToxicityScore());
            dto.put("moderationReason", m.getModerationReason());
            dto.put("createdAt", m.getCreatedAt());
            dto.put("aiProcessed", m.getAiProcessed());
            try {
                Review review = m.getReview();
                if (review != null) {
                    dto.put("reviewId", review.getId());
                    dto.put("reviewText", review.getText() != null ? review.getText() : "");
                    dto.put("reviewStars", review.getStars());
                    dto.put("reviewUsername", review.getUser() != null ? review.getUser().getUsername() : "–");
                    dto.put("reviewMovieTitle", review.getMovie() != null ? review.getMovie().getTitle() : "–");
                    dto.put("reviewMovieId", review.getMovie() != null ? review.getMovie().getId() : null);
                }
            } catch (Exception e) {
                log.warn("Error loading review details for moderation {}: {}", m.getId(), e.getMessage());
            }
            return dto;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var reviewsPage = reviewRepository.findAll(pageable);
        var content = reviewsPage.getContent().stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("text", r.getText() != null ? r.getText() : "");
            dto.put("stars", r.getStars());
            dto.put("createdAt", r.getCreatedAt());
            dto.put("username", r.getUser() != null ? r.getUser().getUsername() : "–");
            dto.put("movieId", r.getMovie() != null ? r.getMovie().getId() : null);
            dto.put("movieTitle", r.getMovie() != null ? r.getMovie().getTitle() : "–");
            dto.put("likesCount", r.getLikesCount());
            return dto;
        }).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", reviewsPage.getTotalElements());
        result.put("totalPages", reviewsPage.getTotalPages());
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reviews/{reviewId}/delete")
    public ResponseEntity<String> deleteReviewAsAdmin(@PathVariable Long reviewId) {
        try {
            Review review = reviewRepository.findById(reviewId).orElse(null);
            if (review == null) return ResponseEntity.notFound().build();
            String username = review.getUser() != null ? review.getUser().getUsername() : "?";
            String movieTitle = review.getMovie() != null ? review.getMovie().getTitle() : "?";
            reviewRepository.delete(review);
            log.info("Admin deleted review {} by user '{}' for movie '{}'", reviewId, username, movieTitle);
            return ResponseEntity.ok("Reseña eliminada correctamente");
        } catch (Exception e) {
            log.error("Error deleting review {}: {}", reviewId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/moderation/{moderationId}/approve")
    public ResponseEntity<String> approveModerationManually(@PathVariable Long moderationId) {
        try {
            return commentModerationRepository.findById(moderationId)
                    .map(moderation -> {
                        moderation.setStatus(CommentModeration.ModerationStatus.APPROVED);
                        moderation.setReviewedAt(java.time.Instant.now());
                        commentModerationRepository.save(moderation);
                        log.info("Moderación {} aprobada manualmente", moderationId);
                        return ResponseEntity.ok("Moderación aprobada exitosamente");
                    })
                    .orElse(ResponseEntity.notFound().<String>build());
        } catch (Exception e) {
            log.error("Error aprobando moderación {}: {}", moderationId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/moderation/{moderationId}/reject")
    public ResponseEntity<String> rejectModerationManually(@PathVariable Long moderationId) {
        try {
            return commentModerationRepository.findById(moderationId)
                    .map(moderation -> {
                        moderation.setStatus(CommentModeration.ModerationStatus.REJECTED);
                        moderation.setReviewedAt(java.time.Instant.now());
                        commentModerationRepository.save(moderation);
                        log.info("Moderación {} rechazada manualmente", moderationId);
                        return ResponseEntity.ok("Moderación rechazada exitosamente");
                    })
                    .orElse(ResponseEntity.notFound().<String>build());
        } catch (Exception e) {
            log.error("Error rechazando moderación {}: {}", moderationId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ============= DEBUG =============

    @GetMapping("/debug/movie/{id}")
    public Map<String, Object> debugMovie(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Movie movie = movieRepository.findByIdWithCastAndDirectors(id).orElse(null);

            if (movie == null) {
                result.put("error", "Película no encontrada");
                return result;
            }

            result.put("movieId", movie.getId());
            result.put("tmdbId", movie.getTmdbId());
            result.put("title", movie.getTitle());
            result.put("actorsCount", movie.getActors().size());
            result.put("directorsCount", movie.getDirectors().size());

            return result;
        } catch (Exception e) {
            result.put("error", "Error: " + e.getMessage());
            return result;
        }
    }

    // ============= SYSTEM HEALTH =============

    @GetMapping("/system/health")
    public ResponseEntity<Map<String, ConnectionStatus>> getSystemHealth() {
        try {
            log.info("🔍 Verificando estado de todas las conexiones del sistema");
            Map<String, ConnectionStatus> healthStatus = systemHealthService.checkAllConnections();
            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            log.error("❌ Error verificando estado del sistema: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", ConnectionStatus.builder()
                    .connected(false)
                    .message("Error verificando estado del sistema: " + e.getMessage())
                    .responseTimeMs(0L)
                    .lastChecked(java.time.Instant.now())
                    .build()
            ));
        }
    }

    @GetMapping("/system/health/{service}")
    public ResponseEntity<ConnectionStatus> getServiceHealth(@PathVariable String service) {
        try {
            log.info("🔍 Verificando estado del servicio: {}", service);

            ConnectionStatus status;
            switch (service.toLowerCase()) {
                case "database":
                    status = systemHealthService.isDatabaseHealthy()
                        ? ConnectionStatus.builder().connected(true).message("Base de datos conectada").build()
                        : ConnectionStatus.builder().connected(false).message("Error en base de datos").build();
                    break;
                case "tmdb":
                    status = systemHealthService.isTmdbHealthy()
                        ? ConnectionStatus.builder().connected(true).message("TMDB API conectada").build()
                        : ConnectionStatus.builder().connected(false).message("Error en TMDB API").build();
                    break;
                case "ollama":
                    status = systemHealthService.isOllamaHealthy()
                        ? ConnectionStatus.builder().connected(true).message("Ollama conectado").build()
                        : ConnectionStatus.builder().connected(false).message("Error en Ollama").build();
                    break;
                case "email":
                    status = ConnectionStatus.builder().connected(true).message("Configuración de email").build();
                    break;
                case "server":
                    status = ConnectionStatus.builder().connected(true).message("Servidor funcionando").build();
                    break;
                default:
                    return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Error verificando estado del servicio {}: {}", service, e.getMessage());
            ConnectionStatus errorStatus = ConnectionStatus.builder()
                .connected(false)
                .message("Error verificando servicio: " + e.getMessage())
                .responseTimeMs(0L)
                .lastChecked(java.time.Instant.now())
                .build();
            return ResponseEntity.status(500).body(errorStatus);
        }
    }
}
