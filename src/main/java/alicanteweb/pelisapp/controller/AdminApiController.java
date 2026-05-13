package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.BookListDTO;
import alicanteweb.pelisapp.dto.ConnectionStatus;
import alicanteweb.pelisapp.dto.TvShowListDTO;
import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.entity.CommentModeration;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.repository.CommentModerationRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.AuthService;
import alicanteweb.pelisapp.service.BookService;
import alicanteweb.pelisapp.service.GoogleBooksLoaderService;
import alicanteweb.pelisapp.service.ModerationService;
import alicanteweb.pelisapp.service.MovieImportService;
import alicanteweb.pelisapp.service.MoviePosterRedownloadService;
import alicanteweb.pelisapp.service.SystemHealthService;
import alicanteweb.pelisapp.service.TMDBMovieLoaderService;
import alicanteweb.pelisapp.service.TMDBSeriesLoaderService;
import alicanteweb.pelisapp.service.TvShowService;
import alicanteweb.pelisapp.service.GoogleBooksClient;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    // Services - Solo los esenciales
    private final TMDBMovieLoaderService tmdbMovieLoaderService;
    private final TMDBSeriesLoaderService tmdbSeriesLoaderService;
    private final ModerationService moderationService;
    private final SystemHealthService systemHealthService;
    private final AuthService authService; // Agregado para búsqueda de usuarios
    private final MoviePosterRedownloadService moviePosterRedownloadService;
    private final MovieImportService movieImportService;
    private final GoogleBooksLoaderService googleBooksLoaderService;
    private final GoogleBooksClient googleBooksClient;
    private final TMDBClient tmdbClient;
    private final TvShowService tvShowService;
    private final BookService bookService;

    // Repositories
    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final BookRepository bookRepository;
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
            user.setBannedUntil(null);
            user.setBanReason(null);
            userRepository.save(user);

            log.info("Usuario ID {} desbaneado", userId);
            return ResponseEntity.ok("Usuario desbaneado exitosamente");

        } catch (Exception e) {
            log.error("Error desbaneando usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/reset-offenses")
    public ResponseEntity<String> resetUserOffenses(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setOffenseCount(0);
            user.setBannedUntil(null);
            user.setBanReason(null);
            userRepository.save(user);

            log.info("Infracciones reseteadas para usuario ID: {}", userId);
            return ResponseEntity.ok("Infracciones reseteadas exitosamente");

        } catch (Exception e) {
            log.error("Error reseteando infracciones para usuario {}: {}", userId, e.getMessage());
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
        var users = userPage.getContent().stream().map(user -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", user.getId());
            m.put("username", user.getUsername());
            m.put("email", user.getEmail());
            m.put("roles", user.getRoles().stream().map(role -> role.getName()).toArray());
            m.put("banned", user.isBanned());
            m.put("emailConfirmed", user.isEmailConfirmed());
            m.put("offenseCount", user.getOffenseCount());
            m.put("bannedUntil", user.getBannedUntil());
            m.put("banReason", user.getBanReason());
            return m;
        }).toList();
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
                    String contentTitle = review.getMovie() != null ? review.getMovie().getTitle()
                            : review.getSeries() != null ? review.getSeries().getTitle()
                            : review.getBook() != null ? review.getBook().getTitle() : "–";
                    dto.put("reviewMovieTitle", contentTitle);
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
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var reviewsPage = reviewRepository.findAll(pageable);
        var content = reviewsPage.getContent().stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("text", r.getText() != null ? r.getText() : "");
            dto.put("stars", r.getStars());
            dto.put("createdAt", r.getCreatedAt());
            dto.put("username", r.getUser() != null ? r.getUser().getUsername() : "–");
            String contentTitle = r.getMovie() != null ? r.getMovie().getTitle()
                    : r.getSeries() != null ? r.getSeries().getTitle()
                    : r.getBook() != null ? r.getBook().getTitle() : "–";
            dto.put("movieId", r.getMovie() != null ? r.getMovie().getId() : null);
            dto.put("movieTitle", contentTitle);
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

    // ============= SERIES MANAGEMENT =============

    @PostMapping("/series/import/{tmdbId}")
    public ResponseEntity<Map<String, Object>> importSeriesFromTMDB(@PathVariable Long tmdbId) {
        try {
            TvShow show = tmdbSeriesLoaderService.importOrUpdateByTmdb(tmdbId);
            if (show != null) {
                return ResponseEntity.ok(Map.of("success", true, "id", show.getId(), "title", show.getTitle()));
            }
            return ResponseEntity.ok(Map.of("success", false, "message", "No se pudo importar la serie"));
        } catch (Exception e) {
            log.error("Error importando serie {}: {}", tmdbId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/series/import-popular")
    public ResponseEntity<Map<String, Object>> importPopularSeries(@RequestParam(defaultValue = "2") int pages) {
        try {
            int count = tmdbSeriesLoaderService.importPopularSeries(Math.min(pages, 5));
            return ResponseEntity.ok(Map.of("success", true, "imported", count));
        } catch (Exception e) {
            log.error("Error importando series populares: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/series/import-top-rated")
    public ResponseEntity<Map<String, Object>> importTopRatedSeries(@RequestParam(defaultValue = "2") int pages) {
        try {
            int count = tmdbSeriesLoaderService.importTopRatedSeries(Math.min(pages, 5));
            return ResponseEntity.ok(Map.of("success", true, "imported", count));
        } catch (Exception e) {
            log.error("Error importando series top rated: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/series")
    public ResponseEntity<Map<String, Object>> listSeries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        var pageable = PageRequest.of(page, size);
        var showPage = (q != null && !q.isBlank())
                ? tvShowRepository.findByTitleContainingIgnoreCase(q.trim(), pageable)
                : tvShowRepository.findAll(pageable);
        var content = showPage.getContent().stream().map(s -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", s.getId());
            dto.put("tmdbId", s.getTmdbId());
            dto.put("title", s.getTitle());
            dto.put("numberOfSeasons", s.getNumberOfSeasons());
            dto.put("genres", s.getGenres());
            dto.put("status", s.getStatus());
            return dto;
        }).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", showPage.getTotalElements());
        result.put("totalPages", showPage.getTotalPages());
        result.put("page", page);
        result.put("query", q);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/series/search-tmdb")
    public ResponseEntity<Map<String, Object>> searchSeriesOnTmdb(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page) {
        try {
            JsonNode response = tmdbClient.searchTv(q, Math.max(1, page));
            List<Map<String, Object>> results = response == null || !response.has("results")
                    ? List.of()
                    : iterableToList(response.path("results")).stream()
                    .map(item -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("tmdbId", item.path("id").asLong());
                        dto.put("title", item.path("name").asText(""));
                        dto.put("originalTitle", item.path("original_name").asText(""));
                        dto.put("firstAirDate", item.path("first_air_date").asText(""));
                        dto.put("overview", item.path("overview").asText(""));
                        dto.put("posterPath", item.path("poster_path").asText(null));
                        dto.put("alreadyImported", tvShowRepository.findByTmdbId(item.path("id").asLong()).isPresent());
                        return dto;
                    }).toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", results,
                    "totalResults", response != null ? response.path("total_results").asInt(0) : 0,
                    "page", response != null ? response.path("page").asInt(page) : page
            ));
        } catch (Exception e) {
            log.error("Error buscando series en TMDB por '{}': {}", q, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============= BOOKS MANAGEMENT =============

    @PostMapping("/books/import/{googleId}")
    public ResponseEntity<Map<String, Object>> importBookFromGoogle(@PathVariable String googleId) {
        try {
            Book book = googleBooksLoaderService.importOrUpdateByGoogleId(googleId);
            if (book != null) {
                return ResponseEntity.ok(Map.of("success", true, "id", book.getId(), "title", book.getTitle()));
            }
            return ResponseEntity.ok(Map.of("success", false, "message", "No se pudo importar el libro"));
        } catch (Exception e) {
            log.error("Error importando libro {}: {}", googleId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/books/search-import")
    public ResponseEntity<Map<String, Object>> searchAndImportBooks(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int maxResults) {
        try {
            int count = googleBooksLoaderService.searchAndImport(q, Math.min(maxResults, 40));
            return ResponseEntity.ok(Map.of("success", true, "imported", count));
        } catch (Exception e) {
            log.error("Error en búsqueda/importación de libros: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/books")
    public ResponseEntity<Map<String, Object>> listBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        var pageable = PageRequest.of(page, size);
        var bookPage = (q != null && !q.isBlank())
                ? bookRepository.findByTitleContainingIgnoreCase(q.trim(), pageable)
                : bookRepository.findAll(pageable);
        var content = bookPage.getContent().stream().map(b -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", b.getId());
            dto.put("googleBooksId", b.getGoogleBooksId());
            dto.put("title", b.getTitle());
            dto.put("authors", b.getAuthors());
            dto.put("publisher", b.getPublisher());
            dto.put("publishedDate", b.getPublishedDate());
            return dto;
        }).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", bookPage.getTotalElements());
        result.put("totalPages", bookPage.getTotalPages());
        result.put("page", page);
        result.put("query", q);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/books/search-google")
    public ResponseEntity<Map<String, Object>> searchBooksOnGoogle(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int startIndex) {
        try {
            JsonNode response = googleBooksClient.searchBooks(q, Math.max(0, startIndex));
            List<Map<String, Object>> results = response == null || !response.has("items")
                    ? List.of()
                    : iterableToList(response.path("items")).stream()
                    .map(item -> {
                        JsonNode info = item.path("volumeInfo");
                        String googleId = item.path("id").asText("");
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("googleBooksId", googleId);
                        dto.put("title", info.path("title").asText(""));
                        dto.put("authors", joinTextArray(info.path("authors")));
                        dto.put("publisher", info.path("publisher").asText(""));
                        dto.put("publishedDate", info.path("publishedDate").asText(""));
                        dto.put("categories", joinTextArray(info.path("categories")));
                        dto.put("coverUrl", firstImageUrl(info.path("imageLinks")));
                        dto.put("alreadyImported", bookRepository.findByGoogleBooksId(googleId).isPresent());
                        return dto;
                    }).toList();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", results,
                    "totalResults", response != null ? response.path("totalItems").asInt(0) : 0,
                    "startIndex", Math.max(0, startIndex)
            ));
        } catch (Exception e) {
            log.error("Error buscando libros en Google Books por '{}': {}", q, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/series/repair-posters")
    public ResponseEntity<Map<String, Object>> repairSeriesPosters() {
        List<TvShow> withoutPosters = tvShowRepository.findAll().stream()
                .filter(s -> s.getPosterLocalPath() == null || s.getPosterLocalPath().isBlank())
                .toList();
        int repaired = 0;
        int errors = 0;
        for (TvShow show : withoutPosters) {
            if (show.getTmdbId() == null) continue;
            try {
                TvShow updated = tmdbSeriesLoaderService.importOrUpdateByTmdb(show.getTmdbId());
                if (updated != null && updated.getPosterLocalPath() != null) repaired++;
                else errors++;
            } catch (Exception e) {
                log.warn("Error repairing poster for series id={}: {}", show.getId(), e.getMessage());
                errors++;
            }
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", withoutPosters.size(),
                "repaired", repaired,
                "errors", errors));
    }

    @PostMapping("/series/{seriesId}/delete")
    public ResponseEntity<String> deleteSeries(@PathVariable Long seriesId) {
        try {
            TvShow show = tvShowRepository.findById(seriesId).orElse(null);
            if (show == null) return ResponseEntity.notFound().build();
            tvShowRepository.delete(show);
            log.info("Admin deleted series id={} title={}", seriesId, show.getTitle());
            return ResponseEntity.ok("Serie eliminada correctamente");
        } catch (Exception e) {
            log.error("Error deleting series {}: {}", seriesId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/books/{bookId}/delete")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        try {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) return ResponseEntity.notFound().build();
            bookRepository.delete(book);
            log.info("Admin deleted book id={} title={}", bookId, book.getTitle());
            return ResponseEntity.ok("Libro eliminado correctamente");
        } catch (Exception e) {
            log.error("Error deleting book {}: {}", bookId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/system/health/{service}")
    public ResponseEntity<ConnectionStatus> getServiceHealth(@PathVariable String service) {
        try {
            log.info("🔍 Verificando estado del servicio: {}", service);

            ConnectionStatus status = switch (service.toLowerCase()) {
                case "database" -> systemHealthService.isDatabaseHealthy()
                        ? ConnectionStatus.builder().connected(true).message("Base de datos conectada").build()
                        : ConnectionStatus.builder().connected(false).message("Error en base de datos").build();
                case "tmdb" -> systemHealthService.isTmdbHealthy()
                        ? ConnectionStatus.builder().connected(true).message("TMDB API conectada").build()
                        : ConnectionStatus.builder().connected(false).message("Error en TMDB API").build();
                case "ollama" -> systemHealthService.isOllamaHealthy()
                        ? ConnectionStatus.builder().connected(true).message("Ollama conectado").build()
                        : ConnectionStatus.builder().connected(false).message("Error en Ollama").build();
                case "email" -> ConnectionStatus.builder().connected(true).message("Configuración de email").build();
                case "server" -> ConnectionStatus.builder().connected(true).message("Servidor funcionando").build();
                default -> null;
            };
            if (status == null) return ResponseEntity.notFound().build();

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

    private List<JsonNode> iterableToList(JsonNode arrayNode) {
        List<JsonNode> items = new java.util.ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            arrayNode.forEach(items::add);
        }
        return items;
    }

    private String joinTextArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return "";
        List<String> values = new java.util.ArrayList<>();
        arrayNode.forEach(node -> values.add(node.asText()));
        return String.join(", ", values);
    }

    private String firstImageUrl(JsonNode imageLinks) {
        if (imageLinks == null || imageLinks.isMissingNode()) return "";
        for (String key : List.of("extraLarge", "large", "medium", "thumbnail", "smallThumbnail")) {
            String url = imageLinks.path(key).asText(null);
            if (url != null && !url.isBlank()) {
                return url.replace("http://", "https://");
            }
        }
        return "";
    }
}
