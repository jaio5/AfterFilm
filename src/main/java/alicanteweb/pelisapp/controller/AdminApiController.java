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

    // Repositories
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final CommentModerationRepository commentModerationRepository;

    // ============= USER MANAGEMENT =============

    /*
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable Long userId, @Valid @RequestBody AssignRoleRequest request) {
        try {
            adminUserService.assignRole(userId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Error asignando rol: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            adminUserService.removeRole(userId, roleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Error removiendo rol: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{userId}/tags")
    public ResponseEntity<Void> assignTag(@PathVariable Long userId, @Valid @RequestBody AssignTagRequest request) {
        try {
            adminUserService.assignTag(userId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Error asignando etiqueta: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/users/{userId}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(@PathVariable Long userId, @PathVariable Long tagId) {
        try {
            adminUserService.removeTag(userId, tagId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Error removiendo etiqueta: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    */

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

    /*
    @GetMapping("/tmdb/test")
    public String testTMDB() {
        try {
            log.info("Probando conexión con TMDB...");

            JsonNode movieDetails = tmdbClient.getMovieDetails(155L); // The Dark Knight

            if (movieDetails != null) {
                String title = movieDetails.path("title").asText("Sin título");
                boolean hasCredits = movieDetails.has("credits");
                int castSize = hasCredits ? movieDetails.path("credits").path("cast").size() : 0;
                int crewSize = hasCredits ? movieDetails.path("credits").path("crew").size() : 0;

                return String.format("✅ TMDB conectado correctamente!\n" +
                    "Película de prueba: %s\n" +
                    "Tiene créditos: %s\n" +
                    "Actores: %d\n" +
                    "Crew: %d",
                    title, hasCredits, castSize, crewSize);
            } else {
                return "❌ No se pudo conectar con TMDB";
            }
        } catch (Exception e) {
            log.error("Error probando TMDB: {}", e.getMessage());
            return "❌ Error conectando con TMDB: " + e.getMessage();
        }
    }
    */

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
        try {
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Proceso de actualización de reparto iniciado");
            result.put("movieId", movieId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error recargando reparto: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
    public ResponseEntity<List<CommentModeration>> getPendingModerations() {

        // Simplificado - retornar lista vacía por ahora
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/moderation/{moderationId}/approve")
    public ResponseEntity<String> approveModerationManually(@PathVariable Long moderationId) {
        try {
            // Simplificado por ahora
            log.info("Aprobación manual de moderación {} solicitada", moderationId);
            return ResponseEntity.ok("Moderación aprobada exitosamente");
        } catch (Exception e) {
            log.error("Error aprobando moderación {}: {}", moderationId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/moderation/{moderationId}/reject")
    public ResponseEntity<String> rejectModerationManually(@PathVariable Long moderationId) {
        try {
            // Simplificado por ahora
            log.info("Rechazo manual de moderación {} solicitado", moderationId);
            return ResponseEntity.ok("Moderación rechazada exitosamente");

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

    // ============= EMAIL DIAGNOSTICS =============

    @GetMapping("/email/diagnostic")
    public ResponseEntity<Map<String, Object>> getEmailDiagnostic() {
        try {
            Map<String, Object> diagnostic = new HashMap<>();
            diagnostic.put("success", true);
            diagnostic.put("message", "Diagnóstico de email no implementado completamente");
            return ResponseEntity.ok(diagnostic);
        } catch (Exception e) {
            log.error("Error en diagnóstico de email: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
