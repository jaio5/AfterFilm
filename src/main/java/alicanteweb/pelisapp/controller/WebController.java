package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.ReviewLikeRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.EmailConfirmationService;
import alicanteweb.pelisapp.service.IEmailService;
import alicanteweb.pelisapp.service.MoviePosterRedownloadService;
import alicanteweb.pelisapp.service.TMDBMovieLoaderService;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador unificado para todas las vistas web HTML
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    // Repositories
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final TvShowRepository tvShowRepository;
    private final BookRepository bookRepository;

    // Services
    private final TMDBMovieLoaderService tmdbMovieLoaderService;
    private final MoviePosterRedownloadService moviePosterRedownloadService;
    private final EmailConfirmationService emailConfirmationService;
    private final IEmailService emailService;
    private final TMDBClient tmdbClient;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private String mailPort;

    @Value("${spring.mail.username:}")
    private String mailUser;



    // ============= ADMIN PAGES =============

    @GetMapping("/admin")
    public String adminIndex(Model model, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        try {
            long totalMovies = movieRepository.count();
            long totalUsers = userRepository.count();
            long totalReviews = reviewRepository.count();
            long totalSeries = tvShowRepository.count();
            long totalBooks = bookRepository.count();
            addAdminStatsToModel(model, totalMovies, totalUsers, totalReviews, totalSeries, totalBooks, auth.getName());
            return "admin/index";
        } catch (Exception e) {
            return handleError(model, "Error cargando panel de admin: " + e.getMessage(), "Error cargando panel de administración");
        }
    }

    @GetMapping("/admin/")
    public String adminDashboardWithSlash() {
        return "redirect:/admin";
    }

    @GetMapping("/admin/movies")
    public String adminMovies(Model model, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        try {
            Page<Movie> moviesPage = movieRepository.findAll(
                PageRequest.of(0, 100, Sort.by("title").ascending()));
            long movieCount = movieRepository.count();
            model.addAttribute("movies", moviesPage.getContent());
            addMovieStatsToModel(model, movieCount);
            return "admin/movies";
        } catch (Exception e) {
            return handleError(model, "Error cargando administración de películas: " + e.getMessage(), "Error cargando películas");
        }
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model, Authentication auth,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(required = false) String search) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> usersPage;
            if (search != null && !search.trim().isEmpty()) {
                usersPage = userRepository.findByUsernameContainingIgnoreCase(search, pageable);
                model.addAttribute("search", search);
            } else {
                usersPage = userRepository.findAll(pageable);
            }
            addUsersPageToModel(model, usersPage, page);
            return "admin/simple-users-fixed";
        } catch (Exception e) {
            return handleError(model, "Error cargando gestión de usuarios: " + e.getMessage(), "Error cargando usuarios");
        }
    }

    @GetMapping("/admin/moderation")
    public String adminModeration(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, "admin/moderation");
        if (redirect != null) return redirect;
        return "admin/moderation";
    }

    @GetMapping("/admin/series")
    public String adminSeries(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        return "admin/series";
    }

    @GetMapping("/admin/books")
    public String adminBooks(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        return "admin/books";
    }

    @GetMapping("/admin/email-config")
    public String adminEmailConfig(Model model, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        model.addAttribute("emailEnabled", emailEnabled);
        model.addAttribute("emailHost", mailHost);
        model.addAttribute("emailPort", mailPort);
        model.addAttribute("emailUser", mailUser);
        return "admin/email-config";
    }

    @PostMapping("/admin/test-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testEmail(
            @RequestParam String email, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos"));
        try {
            emailService.sendConfirmationEmail(email, "admin-test", "TEST_TOKEN_ADMIN");
            return ResponseEntity.ok(Map.of("success", true,
                "message", "✅ Email enviado correctamente a " + email));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false,
                "message", "❌ Error: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/load-popular")
    @ResponseBody
    public ResponseEntity<?> loadPopularMovies(@RequestParam(defaultValue = "3") int pages, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "❌ Sin permisos de administrador"));
        return ResponseEntity.ok(bulkLoadMoviesJson(pages, "popular"));
    }

    @PostMapping("/admin/load-top-rated")
    @ResponseBody
    public ResponseEntity<?> loadTopRatedMovies(@RequestParam(defaultValue = "3") int pages, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "❌ Sin permisos de administrador"));
        return ResponseEntity.ok(bulkLoadMoviesJson(pages, "topRated"));
    }

    @PostMapping("/admin/load-trending")
    @ResponseBody
    public ResponseEntity<?> loadTrendingMovies(@RequestParam(defaultValue = "1") int pages, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "❌ Sin permisos de administrador"));
        return ResponseEntity.ok(loadTrendingMoviesJson());
    }

    @GetMapping("/admin/load-more")
    @ResponseBody
    public String loadMoreMovies(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            String result1 = bulkLoadMovies(5, "popular");
            String result2 = bulkLoadMovies(3, "topRated");
            return "✅ Carga automática completada: " + result1 + " y " + result2;
        } catch (Exception e) {
            log.error("❌ Error en carga automática: {}", e.getMessage());
            return "❌ Error en carga automática: " + e.getMessage();
        }
    }

    @PostMapping("/admin/download-missing-posters")
    @ResponseBody
    public String downloadMissingPosters(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            log.info("🖼️ Iniciando descarga inteligente de carátulas faltantes");
            PosterReloadResult reload = redownloadPosters(false);
            String result = String.format("Caratulas faltantes reparadas: %d. Omitidas: %d. Errores: %d.",
                    reload.reloaded(), reload.skipped(), reload.errors());
            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("❌ Error en descarga inteligente: {}", e.getMessage());
            return "❌ Error en descarga inteligente: " + e.getMessage();
        }
    }

    @PostMapping("/admin/redownload-posters")
    @ResponseBody
    public String redownloadAllPosters(Authentication auth) {
        return reloadMoviePosters(auth); // Usar método existente
    }

    @PostMapping("/admin/redownload-posters-async")
    @ResponseBody
    public String redownloadPostersAsync(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            log.info("🔄 Iniciando redescarga asincrónica de carátulas");
            PosterReloadResult reload = redownloadPosters(true);
            String result = String.format("Caratulas redescargadas: %d. Omitidas: %d. Errores: %d.",
                    reload.reloaded(), reload.skipped(), reload.errors());
            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("❌ Error en redescarga asincrónica: {}", e.getMessage());
            return "❌ Error en redescarga asincrónica: " + e.getMessage();
        }
    }

    @GetMapping("/admin/poster-stats")
    @ResponseBody
    public String getPosterStatistics(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            long totalMovies = movieRepository.count();
            long moviesWithPosters = movieRepository.findAll().stream()
                .mapToLong(movie -> (movie.getPosterLocalPath() != null && !movie.getPosterLocalPath().isBlank()) ? 1 : 0)
                .sum();
            long missingPosters = totalMovies - moviesWithPosters;

            String result = String.format("""
📊 Estadísticas de carátulas:
Total películas: %d
Con carátula: %d (%.1f%%)
Sin carátula: %d (%.1f%%)
""", totalMovies, moviesWithPosters, (moviesWithPosters * 100.0 / totalMovies), missingPosters, (missingPosters * 100.0 / totalMovies));

            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas: {}", e.getMessage());
            return "❌ Error obteniendo estadísticas: " + e.getMessage();
        }
    }

    // ============= ADMIN REVIEW MANAGEMENT =============

    @PostMapping("/admin/review/{reviewId}/delete")
    @ResponseBody
    public String deleteReview(@PathVariable Long reviewId, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";

        try {
            Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
            if (reviewOpt.isEmpty()) {
                return "❌ Reseña no encontrada";
            }

            Review review = reviewOpt.get();
            String username = review.getUser() != null ? review.getUser().getUsername() : "?";
            String movieTitle = review.getMovie() != null ? review.getMovie().getTitle()
                    : review.getSeries() != null ? review.getSeries().getTitle()
                    : review.getBook() != null ? review.getBook().getTitle() : "?";

            // Eliminar la reseña y sus likes asociados
            reviewLikeRepository.deleteByReview_Id(reviewId);
            reviewRepository.delete(review);

            log.info("✅ ADMIN: Reseña eliminada - ID: {}, Usuario: {}, Película: {}, Admin: {}",
                    reviewId, username, movieTitle, auth.getName());

            return "✅ Reseña eliminada exitosamente";

        } catch (Exception e) {
            log.error("❌ Error eliminando reseña {}: {}", reviewId, e.getMessage());
            return "❌ Error eliminando la reseña: " + e.getMessage();
        }
    }

    // ============= UTILITY METHODS =============

    // Método utilitario para obtener y añadir estadísticas de películas
    private void addMovieStatsToModel(Model model, long movieCount) {
        model.addAttribute("movieCount", movieCount);
        model.addAttribute("currentMovieCount", movieCount);
    }

    // Método utilitario para añadir paginación y usuarios al modelo
    private void addUsersPageToModel(Model model, Page<User> usersPage, int page) {
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalElements", usersPage.getTotalElements());
        model.addAttribute("totalUsers", usersPage.getTotalElements());
        model.addAttribute("hasNext", usersPage.hasNext());
        model.addAttribute("hasPrevious", usersPage.hasPrevious());
    }

    // Método utilitario para añadir estadísticas generales al modelo
    private void addAdminStatsToModel(Model model, long totalMovies, long totalUsers, long totalReviews,
                                       long totalSeries, long totalBooks, String adminUser) {
        model.addAttribute("totalMovies", totalMovies);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("totalSeries", totalSeries);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("adminUser", adminUser);
    }

    private boolean isNotAdmin(Authentication auth) {
        return auth == null || auth.getAuthorities().stream()
            .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // Método utilitario para comprobar permisos de admin y redirigir si no lo es
    private String requireAdminOrRedirect(Authentication auth, String viewIfAdmin) {
        if (isNotAdmin(auth)) {
            return "redirect:/login";
        }
        return viewIfAdmin;
    }


    // Método utilitario para la carga masiva de películas
    private String bulkLoadMovies(int pages, String type) {
        try {
            log.info("🚀 INICIANDO CARGA MASIVA: Tipo={}, Páginas={}", type, pages);
            long countBefore = movieRepository.count();
            log.info("📊 ANTES DE LA CARGA: {} películas en base de datos", countBefore);

            if ("popular".equals(type)) {
                log.info("📽️ Llamando a tmdbMovieLoaderService.loadPopularMovies({})", Math.min(pages, 5));
                tmdbMovieLoaderService.loadPopularMovies(Math.min(pages, 5));
            } else if ("topRated".equals(type)) {
                log.info("⭐ Llamando a tmdbMovieLoaderService.loadTopRatedMovies({})", Math.min(pages, 5));
                tmdbMovieLoaderService.loadTopRatedMovies(Math.min(pages, 5));
            } else {
                throw new IllegalArgumentException("Tipo de carga no soportado");
            }

            long countAfter = movieRepository.count();
            long newMovies = countAfter - countBefore;
            log.info("📊 DESPUÉS DE LA CARGA: {} películas en base de datos (+{} nuevas)", countAfter, newMovies);

            String label = "popular".equals(type) ? "películas populares" : "películas top rated";
            String result = String.format("✅ Se han cargado %d nuevas %s desde TMDB (Total: %d → %d)",
                    newMovies, label, countBefore, countAfter);
            log.info("✅ RESULTADO FINAL: {}", result);
            return result;
        } catch (Exception e) {
            log.error("❌ ERROR EN CARGA MASIVA: Tipo={}, Páginas={}, Error={}", type, pages, e.getMessage(), e);
            return "❌ Error cargando películas: " + e.getMessage();
        }
    }

    private String bulkLoadMoviesUntilTarget(int targetMovies, String type) {
        try {
            MovieLoadResult result = loadMoviesUntilTarget(targetMovies, type);
            return formatMovieLoadResult(result);
        } catch (Exception e) {
            log.error("❌ ERROR EN CARGA POR OBJETIVO: Tipo={}, Objetivo={}, Error={}",
                    type, targetMovies, e.getMessage(), e);
            return "❌ Error cargando películas: " + e.getMessage();
        }
    }

    private String bulkLoadCombinedMoviesUntilTarget(int targetMovies) {
        try {
            int firstTarget = Math.max(1, targetMovies / 2);
            MovieLoadResult popular = loadMoviesUntilTarget(firstTarget, "popular");
            int remainingTarget = Math.max(0, targetMovies - popular.imported());
            MovieLoadResult topRated = remainingTarget > 0
                    ? loadMoviesUntilTarget(remainingTarget, "topRated")
                    : MovieLoadResult.empty("topRated", movieRepository.count());

            MovieLoadResult combined = popular.combine(topRated, targetMovies, "popular + top rated");
            return formatMovieLoadResult(combined);
        } catch (Exception e) {
            log.error("❌ ERROR EN CARGA COMBINADA POR OBJETIVO: Objetivo={}, Error={}",
                    targetMovies, e.getMessage(), e);
            return "❌ Error cargando películas: " + e.getMessage();
        }
    }

    private MovieLoadResult loadMoviesUntilTarget(int targetMovies, String type) {
        int safeTarget = Math.max(1, targetMovies);
        long totalBefore = movieRepository.count();
        int imported = 0;
        int omitted = 0;
        int errors = 0;
        int pagesChecked = 0;
        int maxPages = 500; // TMDB limita los listados paginados a 500 páginas.

        log.info("🚀 INICIANDO CARGA POR OBJETIVO: Tipo={}, Objetivo={} películas nuevas", type, safeTarget);

        for (int page = 1; imported < safeTarget && page <= maxPages; page++) {
            JsonNode response = fetchMovieListPage(type, page);
            if (response == null || !response.has("results")) {
                log.warn("TMDB no devolvió resultados para tipo={} página={}", type, page);
                break;
            }

            int totalPages = response.path("total_pages").asInt(maxPages);
            maxPages = Math.min(maxPages, Math.max(1, totalPages));
            JsonNode results = response.path("results");
            if (!results.isArray() || results.isEmpty()) {
                break;
            }

            pagesChecked++;
            for (JsonNode movieNode : results) {
                if (imported >= safeTarget) {
                    break;
                }

                long tmdbId = movieNode.path("id").asLong(0);
                if (tmdbId == 0) {
                    errors++;
                    continue;
                }

                try {
                    if (movieRepository.findByTmdbId(tmdbId).isPresent()) {
                        omitted++;
                        continue;
                    }
                    Movie movie = tmdbMovieLoaderService.loadMovieByTmdbId(tmdbId);
                    if (movie != null) {
                        imported++;
                    } else {
                        errors++;
                    }
                } catch (Exception e) {
                    errors++;
                    log.warn("Error importando película tmdbId={} en carga por objetivo: {}", tmdbId, e.getMessage());
                }
            }
        }

        long totalAfter = movieRepository.count();
        return new MovieLoadResult(type, safeTarget, imported, omitted, errors, pagesChecked, totalBefore, totalAfter);
    }

    private JsonNode fetchMovieListPage(String type, int page) {
        return switch (type) {
            case "popular" -> tmdbClient.getPopular(page);
            case "topRated" -> tmdbClient.getTopRated(page);
            default -> throw new IllegalArgumentException("Tipo de carga no soportado: " + type);
        };
    }

    private String formatMovieLoadResult(MovieLoadResult result) {
        String suffix = result.imported() >= result.target()
                ? ""
                : " No se llegó al objetivo porque no había más resultados disponibles en esa fuente o TMDB devolvió errores.";
        return String.format(
                "✅ Se han cargado %d nuevas películas de %d solicitadas (%s). Omitidas por existir: %d. Errores: %d. Páginas revisadas: %d. Total: %d → %d.%s",
                result.imported(), result.target(), result.label(), result.omitted(), result.errors(),
                result.pagesChecked(), result.totalBefore(), result.totalAfter(), suffix);
    }

    private record MovieLoadResult(
            String label,
            int target,
            int imported,
            int omitted,
            int errors,
            int pagesChecked,
            long totalBefore,
            long totalAfter
    ) {
        static MovieLoadResult empty(String label, long total) {
            return new MovieLoadResult(label, 0, 0, 0, 0, 0, total, total);
        }

        MovieLoadResult combine(MovieLoadResult other, int combinedTarget, String combinedLabel) {
            return new MovieLoadResult(
                    combinedLabel,
                    combinedTarget,
                    imported + other.imported(),
                    omitted + other.omitted(),
                    errors + other.errors(),
                    pagesChecked + other.pagesChecked(),
                    totalBefore,
                    other.totalAfter()
            );
        }
    }

    /**
     * Versión mejorada: devuelve un JSON con el resultado de la carga masiva
     */
    private Map<String, Object> bulkLoadMoviesJson(int pages, String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("🚀 INICIANDO CARGA MASIVA: Tipo={}, Páginas={}", type, pages);
            long countBefore = movieRepository.count();
            int totalProcessed = 0;
            int totalOmitted = 0;
            int totalErrors = 0;
            if ("popular".equals(type)) {
                for (int page = 1; page <= Math.min(pages, 5); page++) {
                    try {
                        int processed = tmdbMovieLoaderService.loadPopularMoviesAndReturnCount(page);
                        totalProcessed += processed;
                        totalOmitted += 20 - processed; // asumiendo 20 por página
                    } catch (Exception e) {
                        totalErrors++;
                    }
                }
            } else if ("topRated".equals(type)) {
                for (int page = 1; page <= Math.min(pages, 5); page++) {
                    try {
                        int processed = tmdbMovieLoaderService.loadTopRatedMoviesAndReturnCount(page);
                        totalProcessed += processed;
                        totalOmitted += 20 - processed;
                    } catch (Exception e) {
                        totalErrors++;
                    }
                }
            } else {
                throw new IllegalArgumentException("Tipo de carga no soportado");
            }
            long countAfter = movieRepository.count();
            long newMovies = countAfter - countBefore;
            result.put("success", true);
            result.put("newMovies", newMovies);
            result.put("processed", totalProcessed);
            result.put("omitted", totalOmitted);
            result.put("errors", totalErrors);
            result.put("totalBefore", countBefore);
            result.put("totalAfter", countAfter);
            result.put("message", String.format("Se han cargado %d nuevas películas. Omitidas: %d. Errores: %d.", newMovies, totalOmitted, totalErrors));
        } catch (Exception e) {
            log.error("❌ ERROR EN CARGA MASIVA: Tipo={}, Páginas={}, Error={}", type, pages, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "❌ Error cargando películas: " + e.getMessage());
        }
        return result;
    }

    // Método utilitario para manejo de errores en endpoints
    private String handleError(Model model, String logMsg, String userMsg) {
        log.error(logMsg);
        model.addAttribute("error", userMsg);
        return "error";
    }

    private Map<String, Object> loadTrendingMoviesJson() {
        Map<String, Object> result = new HashMap<>();
        try {
            long countBefore = movieRepository.count();
            JsonNode response = tmdbClient.getTrending("movie", "week");
            int processed = 0;
            int omitted = 0;
            int errors = 0;

            if (response != null && response.has("results")) {
                for (JsonNode movieNode : response.path("results")) {
                    long tmdbId = movieNode.path("id").asLong(0);
                    if (tmdbId == 0) {
                        errors++;
                        continue;
                    }
                    try {
                        if (movieRepository.findByTmdbId(tmdbId).isPresent()) {
                            omitted++;
                            continue;
                        }
                        Movie movie = tmdbMovieLoaderService.loadMovieByTmdbId(tmdbId);
                        if (movie != null) processed++;
                        else errors++;
                    } catch (Exception e) {
                        errors++;
                        log.warn("Error importando pelicula en tendencia tmdbId={}: {}", tmdbId, e.getMessage());
                    }
                }
            }

            long countAfter = movieRepository.count();
            long newMovies = countAfter - countBefore;
            result.put("success", true);
            result.put("newMovies", newMovies);
            result.put("processed", processed);
            result.put("omitted", omitted);
            result.put("errors", errors);
            result.put("totalBefore", countBefore);
            result.put("totalAfter", countAfter);
            result.put("message", String.format("Se han cargado %d nuevas peliculas en tendencia. Omitidas: %d. Errores: %d.",
                    newMovies, omitted, errors));
        } catch (Exception e) {
            log.error("Error cargando peliculas en tendencia: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error cargando peliculas en tendencia: " + e.getMessage());
        }
        return result;
    }

    private PosterReloadResult redownloadPosters(boolean includeExisting) {
        int reloaded = 0;
        int skipped = 0;
        int errors = 0;

        for (Movie movie : movieRepository.findAll()) {
            boolean hasLocalPoster = movie.getPosterLocalPath() != null && !movie.getPosterLocalPath().isBlank();
            if (!includeExisting && hasLocalPoster) {
                skipped++;
                continue;
            }
            try {
                if (moviePosterRedownloadService.redownloadMoviePoster(movie)) {
                    reloaded++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Error redescargando caratula de pelicula id={}: {}", movie.getId(), e.getMessage());
            }
        }

        return new PosterReloadResult(reloaded, skipped, errors);
    }

    private record PosterReloadResult(int reloaded, int skipped, int errors) {}

    // Método utilitario para presets de carga masiva (switch mejorado)
    private String handlePreset(String presetName) {
        return switch (presetName.toLowerCase()) {
            case "quick" -> bulkLoadMoviesUntilTarget(200, "popular");
            case "medium" -> bulkLoadMoviesUntilTarget(1000, "popular");
            case "full" -> bulkLoadMoviesUntilTarget(4000, "popular");
            case "ultimate" -> bulkLoadMoviesUntilTarget(10000, "popular");
            case "categories" -> bulkLoadCombinedMoviesUntilTarget(1000);
            default -> "❌ Preset no válido: " + presetName;
        };
    }


    @PostMapping("/resend-confirmation")
    @ResponseBody
    public Map<String, Object> resendConfirmation(@RequestParam String email) {
        EmailConfirmationService.EmailConfirmationResult result = emailConfirmationService.resendConfirmationEmail(email);
        return Map.of(
            "success", result.success(),
            "message", result.message()
        );
    }


    @PostMapping("/admin/reload-posters")
    @ResponseBody
    public String reloadMoviePosters(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            log.info("🖼️ Iniciando recarga de posters de películas");
            PosterReloadResult reload = redownloadPosters(true);
            String result = String.format("Caratulas redescargadas: %d. Omitidas: %d. Errores: %d.",
                    reload.reloaded(), reload.skipped(), reload.errors());
            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("❌ Error recargando posters: {}", e.getMessage());
            return "❌ Error recargando posters: " + e.getMessage();
        }
    }

    @PostMapping("/admin/bulk-loader/preset/{presetName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> usePreset(@PathVariable String presetName, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));
        Map<String, Object> response = new HashMap<>();
        try {
            String message = handlePreset(presetName);
            if (message.startsWith("❌")) {
                response.put("success", false);
                response.put("message", message);
            } else {
                response.put("success", true);
                response.put("message", message);
                response.put("preset", presetName);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error ejecutando preset {}: {}", presetName, e.getMessage());
            response.put("success", false);
            response.put("message", "Error ejecutando preset: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/admin/bulk-loader/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBulkLoaderStatus(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));
        try {
            Map<String, Object> status = new HashMap<>();
            long movieCount = movieRepository.count();
            status.put("success", true);
            status.put("movieCount", movieCount);
            status.put("currentMovieCount", movieCount);
            status.put("isLoading", false);
            status.put("lastUpdate", System.currentTimeMillis());
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Error obteniendo estado: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error obteniendo estado: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/bulk-loader/start-popular")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startPopularBulkLoad(
            @RequestParam(defaultValue = "1000") int targetMovies,
            Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));

        Map<String, Object> response = new HashMap<>();
        try {
            int safeTarget = Math.min(Math.max(1, targetMovies), 10000);
            log.info("🚀 Iniciando carga popular personalizada: {} películas nuevas", safeTarget);
            String result = bulkLoadMoviesUntilTarget(safeTarget, "popular");
            response.put("success", true);
            response.put("message", result);
            response.put("targetMovies", safeTarget);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error en carga popular personalizada: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Error iniciando carga: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/admin/bulk-loader/start-categories")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startCategoriesBulkLoad(
            @RequestParam(defaultValue = "1000") int targetMovies,
            Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));

        Map<String, Object> response = new HashMap<>();
        try {
            int safeTarget = Math.min(Math.max(1, targetMovies), 10000);
            log.info("🚀 Iniciando carga por categorías: {} películas nuevas", safeTarget);
            String result = bulkLoadCombinedMoviesUntilTarget(safeTarget);

            response.put("success", true);
            response.put("message", result);
            response.put("targetMovies", safeTarget);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error en carga por categorías: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Error iniciando carga por categorías: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/admin/bulk-loader/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBulkLoad(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));

        Map<String, Object> response = new HashMap<>();
        try {
            log.info("🛑 Solicitud de cancelación de carga masiva");
            response.put("success", true);
            response.put("message", "Carga cancelada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error cancelando carga: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Error cancelando carga: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/admin/bulk-loader")
    public String adminBulkLoader(Model model, Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return redirect;
        try {
            long movieCount = movieRepository.count();
            addMovieStatsToModel(model, movieCount);
            return "admin/bulk-loader";
        } catch (Exception e) {
            return handleError(model, "Error cargando bulk loader: " + e.getMessage(), "Error cargando bulk loader");
        }
    }

    @PostMapping("/admin/redownload-cast-director-images")
    @ResponseBody
    public String redownloadCastDirectorImages(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            int total = tmdbMovieLoaderService.redownloadCastDirectorImages();
            return "✅ Redescarga de imágenes de reparto y director completada: " + total + " imágenes";
        } catch (Exception e) {
            log.error("❌ Error en redescarga de reparto/director: {}", e.getMessage());
            return "❌ Error en redescarga de reparto/director: " + e.getMessage();
        }
    }

    @PostMapping("/admin/delete-duplicate-images")
    @ResponseBody
    public String deleteDuplicateImages(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            int deleted = tmdbMovieLoaderService.deleteDuplicateImages();
            return "✅ Eliminadas " + deleted + " fotos duplicadas";
        } catch (Exception e) {
            log.error("❌ Error eliminando duplicados: {}", e.getMessage());
            return "❌ Error eliminando duplicados: " + e.getMessage();
        }
    }
}
