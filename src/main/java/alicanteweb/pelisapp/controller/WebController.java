package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.ReviewLikeRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.ContentRepairService;
import alicanteweb.pelisapp.service.EmailConfirmationService;
import alicanteweb.pelisapp.service.GoogleBooksClient;
import alicanteweb.pelisapp.service.GoogleBooksLoaderService;
import alicanteweb.pelisapp.service.IEmailService;
import alicanteweb.pelisapp.service.MoviePosterRedownloadService;
import alicanteweb.pelisapp.service.TMDBMovieLoaderService;
import alicanteweb.pelisapp.service.TMDBSeriesLoaderService;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

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
    private final TMDBSeriesLoaderService tmdbSeriesLoaderService;
    private final GoogleBooksLoaderService googleBooksLoaderService;
    private final GoogleBooksClient googleBooksClient;
    private final MoviePosterRedownloadService moviePosterRedownloadService;
    private final ContentRepairService contentRepairService;
    private final EmailConfirmationService emailConfirmationService;
    private final IEmailService emailService;
    private final TMDBClient tmdbClient;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.provider:resend}")
    private String emailProvider;

    @Value("${spring.mail.from:}")
    private String emailFrom;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    private final ExecutorService bulkLoaderExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean bulkLoading = new AtomicBoolean(false);
    private final AtomicBoolean bulkCancelRequested = new AtomicBoolean(false);
    private final AtomicInteger bulkTargetMovies = new AtomicInteger(0);
    private final AtomicInteger bulkImportedMovies = new AtomicInteger(0);
    private final AtomicInteger bulkOmittedMovies = new AtomicInteger(0);
    private final AtomicInteger bulkErrorMovies = new AtomicInteger(0);
    private final AtomicInteger bulkPagesChecked = new AtomicInteger(0);
    private final AtomicLong bulkStartedAt = new AtomicLong(0);
    private volatile String bulkTaskName = "";
    private volatile String bulkCurrentSource = "";
    private volatile String bulkLastMessage = "Sistema iniciado — listo para cargar películas";

    @PreDestroy
    public void shutdownBulkLoaderExecutor() {
        bulkLoaderExecutor.shutdownNow();
    }



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
            int safePage = EndpointSanitizer.page(page);
            int safeSize = EndpointSanitizer.size(size, 20, 100);
            String safeSearch = EndpointSanitizer.optionalText(search, 80);
            Pageable pageable = PageRequest.of(safePage, safeSize);
            Page<User> usersPage;
            if (safeSearch != null) {
                usersPage = userRepository.findByUsernameContainingIgnoreCase(safeSearch, pageable);
                model.addAttribute("search", safeSearch);
            } else {
                usersPage = userRepository.findAll(pageable);
            }
            addUsersPageToModel(model, usersPage, safePage);
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
        model.addAttribute("emailProvider", emailProvider);
        model.addAttribute("emailFrom", emailFrom);
        model.addAttribute("resendConfigured", resendApiKey != null && !resendApiKey.isBlank());
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
            List<Movie> movies = movieRepository.findAll();
            long moviesWithLocalPosters = movies.stream()
                    .filter(this::hasLocalPoster)
                    .count();
            long moviesWithRemotePostersOnly = movies.stream()
                    .filter(movie -> !hasLocalPoster(movie) && hasRemotePoster(movie))
                    .count();
            long moviesWithAnyPoster = moviesWithLocalPosters + moviesWithRemotePostersOnly;
            long missingPosters = totalMovies - moviesWithAnyPoster;

            String result = String.format("""
📊 Estadísticas de carátulas:
Total películas: %d
Con carátula visible: %d (%.1f%%)
Guardadas localmente: %d
Solo remotas (TMDB): %d
Sin carátula: %d (%.1f%%)
""",
                    totalMovies,
                    moviesWithAnyPoster,
                    percentage(moviesWithAnyPoster, totalMovies),
                    moviesWithLocalPosters,
                    moviesWithRemotePostersOnly,
                    missingPosters,
                    percentage(missingPosters, totalMovies));

            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas: {}", e.getMessage());
            return "❌ Error obteniendo estadísticas: " + e.getMessage();
        }
    }

    private boolean hasLocalPoster(Movie movie) {
        return movie.getPosterLocalPath() != null && !movie.getPosterLocalPath().isBlank();
    }

    private boolean hasRemotePoster(Movie movie) {
        return movie.getPosterPath() != null && !movie.getPosterPath().isBlank();
    }

    private boolean hasVisiblePoster(Movie movie) {
        return hasLocalPoster(movie) || hasRemotePoster(movie);
    }

    private double percentage(long value, long total) {
        return total > 0 ? value * 100.0 / total : 0.0;
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
            String[] sources = {
                    "popular",
                    "topRated",
                    "nowPlaying",
                    "upcoming",
                    "discoverPopular",
                    "discoverVoteCount",
                    "discoverReleaseDate",
                    "discoverRevenue"
            };
            MovieLoadResult combined = MovieLoadResult.empty("varias fuentes TMDB", movieRepository.count());

            for (String source : sources) {
                if (bulkCancelRequested.get()) {
                    break;
                }
                int remainingTarget = Math.max(0, targetMovies - combined.imported());
                if (remainingTarget == 0) {
                    break;
                }
                MovieLoadResult partial = loadMoviesUntilTarget(remainingTarget, source);
                combined = combined.combine(partial, targetMovies, "varias fuentes TMDB");
            }

            return formatMovieLoadResult(combined);
        } catch (Exception e) {
            log.error("❌ ERROR EN CARGA COMBINADA POR OBJETIVO: Objetivo={}, Error={}",
                    targetMovies, e.getMessage(), e);
            return "❌ Error cargando películas: " + e.getMessage();
        }
    }

    private String bulkLoadContentUntilTarget(String contentType, int targetItems) {
        return switch (contentType.toLowerCase()) {
            case "movie", "movies", "peliculas", "películas" -> bulkLoadCombinedMoviesUntilTarget(targetItems);
            case "series", "tv" -> bulkLoadCombinedSeriesUntilTarget(targetItems);
            case "book", "books", "libros" -> bulkLoadCombinedBooksUntilTarget(targetItems);
            default -> "❌ Tipo de contenido no válido: " + contentType;
        };
    }

    private String bulkLoadCombinedSeriesUntilTarget(int targetSeries) {
        String[] sources = {"popular", "topRated", "trending"};
        ContentLoadResult combined = ContentLoadResult.empty("varias fuentes TMDB series", tvShowRepository.count());
        for (String source : sources) {
            if (bulkCancelRequested.get() || combined.imported() >= targetSeries) {
                break;
            }
            int remaining = Math.max(0, targetSeries - combined.imported());
            ContentLoadResult partial = loadSeriesUntilTarget(remaining, source);
            combined = combined.combine(partial, targetSeries, "varias fuentes TMDB series");
        }
        return formatContentLoadResult(combined, "series");
    }

    private ContentLoadResult loadSeriesUntilTarget(int targetSeries, String source) {
        int safeTarget = Math.max(1, targetSeries);
        long totalBefore = tvShowRepository.count();
        int imported = 0;
        int omitted = 0;
        int errors = 0;
        int pagesChecked = 0;
        int maxPages = "trending".equals(source) ? 1 : 500;
        bulkCurrentSource = "series:" + source;

        for (int page = 1; imported < safeTarget && page <= maxPages && !bulkCancelRequested.get(); page++) {
            JsonNode response = fetchSeriesListPage(source, page);
            if (response == null || !response.has("results")) {
                break;
            }
            int totalPages = response.path("total_pages").asInt(maxPages);
            maxPages = Math.min(maxPages, Math.max(1, totalPages));
            JsonNode results = response.path("results");
            if (!results.isArray() || results.isEmpty()) {
                break;
            }

            pagesChecked++;
            bulkPagesChecked.incrementAndGet();
            updateBulkProgress(String.format("Revisando series %s página %d. Nuevas: %d/%d. Omitidas: %d. Errores: %d.",
                    source, page, bulkImportedMovies.get(), bulkTargetMovies.get(), bulkOmittedMovies.get(), bulkErrorMovies.get()));

            for (JsonNode seriesNode : results) {
                if (imported >= safeTarget || bulkCancelRequested.get()) {
                    break;
                }
                long tmdbId = seriesNode.path("id").asLong(0);
                if (tmdbId == 0) {
                    errors++;
                    bulkErrorMovies.incrementAndGet();
                    continue;
                }
                try {
                    if (tvShowRepository.findByTmdbId(tmdbId).isPresent()) {
                        omitted++;
                        bulkOmittedMovies.incrementAndGet();
                        continue;
                    }
                    TvShow series = tmdbSeriesLoaderService.importOrUpdateByTmdb(tmdbId);
                    if (series != null) {
                        imported++;
                        bulkImportedMovies.incrementAndGet();
                        updateBulkProgress(String.format("Serie procesada: %s. Total: %d/%d.",
                                series.getTitle(), bulkImportedMovies.get(), bulkTargetMovies.get()));
                    } else {
                        errors++;
                        bulkErrorMovies.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors++;
                    bulkErrorMovies.incrementAndGet();
                    log.warn("Error importando serie tmdbId={}: {}", tmdbId, e.getMessage());
                }
            }
        }
        return new ContentLoadResult(source, safeTarget, imported, omitted, errors, pagesChecked, totalBefore, tvShowRepository.count());
    }

    private JsonNode fetchSeriesListPage(String source, int page) {
        return switch (source) {
            case "popular" -> tmdbClient.getTvPopular(page);
            case "topRated" -> tmdbClient.getTvTopRated(page);
            case "trending" -> tmdbClient.getTrending("tv", "week");
            default -> throw new IllegalArgumentException("Tipo de carga de series no soportado: " + source);
        };
    }

    private String bulkLoadCombinedBooksUntilTarget(int targetBooks) {
        List<String> queries = buildBookBulkQueries();
        ContentLoadResult combined = ContentLoadResult.empty("varias búsquedas Google Books", bookRepository.count());
        for (String query : queries) {
            if (bulkCancelRequested.get() || combined.imported() >= targetBooks) {
                break;
            }
            int remaining = Math.max(0, targetBooks - combined.imported());
            ContentLoadResult partial = loadBooksUntilTarget(remaining, query);
            combined = combined.combine(partial, targetBooks, "varias búsquedas Google Books");
        }
        return formatContentLoadResult(combined, "libros");
    }

    private List<String> buildBookBulkQueries() {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        List.of(
                "fiction", "literary collections", "science fiction", "fantasy", "history", "biography",
                "mystery", "romance", "thriller", "horror", "philosophy", "technology", "science",
                "art", "business", "economics", "cooking", "travel", "poetry", "drama", "comics",
                "graphic novels", "juvenile fiction", "young adult fiction", "self-help", "health",
                "education", "music", "religion", "sports", "computers", "psychology", "nature",
                "law", "medical", "political science", "social science", "performing arts", "humor"
        ).forEach(subject -> queries.add("subject:" + subject));

        List.of(
                "novela", "novela española", "literatura", "historia", "ciencia", "poesia", "poesía",
                "teatro", "ensayo", "aventura", "infantil", "juvenil", "cocina", "arte", "filosofia",
                "filosofía", "tecnologia", "tecnología", "negocios", "amor", "vida", "mundo", "viaje",
                "familia", "guerra", "memorias", "cuentos", "relatos", "misterio", "terror", "fantasia",
                "fantasía", "aprendizaje", "salud", "educacion", "educación", "musica", "música",
                "a", "the", "of", "life", "world", "love", "story", "new", "guide", "history",
                "science", "art", "business", "children", "adventure", "essays", "poems"
        ).forEach(queries::add);

        for (char ch = 'a'; ch <= 'z'; ch++) {
            queries.add("intitle:" + ch);
        }
        for (char ch = 'a'; ch <= 'z'; ch++) {
            queries.add("inauthor:" + ch);
        }
        return new ArrayList<>(queries);
    }

    private ContentLoadResult loadBooksUntilTarget(int targetBooks, String query) {
        int safeTarget = Math.max(1, targetBooks);
        long totalBefore = bookRepository.count();
        int imported = 0;
        int omitted = 0;
        int errors = 0;
        int pagesChecked = 0;
        int startIndex = 0;
        int pagesWithoutNewBooks = 0;
        bulkCurrentSource = "libros:" + query;

        while (imported < safeTarget && startIndex < 2000 && !bulkCancelRequested.get()) {
            String orderBy = startIndex < 1000 ? "relevance" : "newest";
            int effectiveStartIndex = startIndex % 1000;
            JsonNode response = googleBooksClient.searchBooks(query, effectiveStartIndex, 40, orderBy);
            if (response == null || !response.has("items")) {
                break;
            }
            pagesChecked++;
            bulkPagesChecked.incrementAndGet();
            updateBulkProgress(String.format("Revisando libros %s (%s) desde %d. Nuevos: %d/%d. Omitidos: %d. Errores: %d.",
                    query, orderBy, effectiveStartIndex, bulkImportedMovies.get(), bulkTargetMovies.get(), bulkOmittedMovies.get(), bulkErrorMovies.get()));

            int importedBeforePage = imported;
            for (JsonNode item : response.path("items")) {
                if (imported >= safeTarget || bulkCancelRequested.get()) {
                    break;
                }
                String googleId = item.path("id").asText(null);
                if (googleId == null || googleId.isBlank()) {
                    errors++;
                    bulkErrorMovies.incrementAndGet();
                    continue;
                }
                try {
                    if (bookRepository.findByGoogleBooksId(googleId).isPresent()) {
                        omitted++;
                        bulkOmittedMovies.incrementAndGet();
                        continue;
                    }
                    Book book = googleBooksLoaderService.importOrUpdateFromSearchItem(item);
                    if (book == null) {
                        book = googleBooksLoaderService.importOrUpdateByGoogleId(googleId);
                    }
                    if (book != null) {
                        imported++;
                        bulkImportedMovies.incrementAndGet();
                        updateBulkProgress(String.format("Libro procesado: %s. Total: %d/%d.",
                                book.getTitle(), bulkImportedMovies.get(), bulkTargetMovies.get()));
                    } else {
                        errors++;
                        bulkErrorMovies.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors++;
                    bulkErrorMovies.incrementAndGet();
                    log.warn("Error importando libro googleId={}: {}", googleId, e.getMessage());
                }
            }

            if (imported == importedBeforePage) {
                pagesWithoutNewBooks++;
                if (pagesWithoutNewBooks >= 5) {
                    updateBulkProgress(String.format("Sin libros nuevos en %s tras %d páginas; saltando a otra fuente.", query, pagesWithoutNewBooks));
                    break;
                }
            } else {
                pagesWithoutNewBooks = 0;
            }

            int totalItems = response.path("totalItems").asInt(0);
            startIndex += 40;
            int providerLimit = Math.min(Math.max(totalItems, 0), 2000);
            if (providerLimit > 0 && startIndex >= providerLimit) {
                break;
            }
        }

        return new ContentLoadResult(query, safeTarget, imported, omitted, errors, pagesChecked, totalBefore, bookRepository.count());
    }

    private String formatContentLoadResult(ContentLoadResult result, String noun) {
        String suffix = result.imported() >= result.target()
                ? ""
                : " No se llegó al objetivo porque no había más resultados disponibles o la API devolvió errores.";
        return String.format(
                "✅ Se han cargado %d nuevos %s de %d solicitados (%s). Omitidos por existir: %d. Errores: %d. Páginas revisadas: %d. Total: %d → %d.%s",
                result.imported(), noun, result.target(), result.label(), result.omitted(), result.errors(),
                result.pagesChecked(), result.totalBefore(), result.totalAfter(), suffix);
    }

    private record ContentLoadResult(
            String label,
            int target,
            int imported,
            int omitted,
            int errors,
            int pagesChecked,
            long totalBefore,
            long totalAfter
    ) {
        static ContentLoadResult empty(String label, long total) {
            return new ContentLoadResult(label, 0, 0, 0, 0, 0, total, total);
        }

        ContentLoadResult combine(ContentLoadResult other, int combinedTarget, String combinedLabel) {
            return new ContentLoadResult(
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

    private MovieLoadResult loadMoviesUntilTarget(int targetMovies, String type) {
        int safeTarget = Math.max(1, targetMovies);
        long totalBefore = movieRepository.count();
        int imported = 0;
        int omitted = 0;
        int errors = 0;
        int pagesChecked = 0;
        int maxPages = 500; // TMDB limita los listados paginados a 500 páginas.

        log.info("🚀 INICIANDO CARGA POR OBJETIVO: Tipo={}, Objetivo={} películas nuevas", type, safeTarget);
        bulkCurrentSource = type;

        for (int page = 1; imported < safeTarget && page <= maxPages && !bulkCancelRequested.get(); page++) {
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
            bulkPagesChecked.incrementAndGet();
            updateBulkProgress(String.format("Revisando %s página %d. Nuevas: %d/%d. Omitidas: %d. Errores: %d.",
                    type, page, bulkImportedMovies.get(), bulkTargetMovies.get(), bulkOmittedMovies.get(), bulkErrorMovies.get()));
            for (JsonNode movieNode : results) {
                if (imported >= safeTarget || bulkCancelRequested.get()) {
                    break;
                }

                long tmdbId = movieNode.path("id").asLong(0);
                if (tmdbId == 0) {
                    errors++;
                    bulkErrorMovies.incrementAndGet();
                    continue;
                }

                try {
                    // Carga masiva: intentar cargar sin importar si ya existe
                    // El objetivo es completar hasta X películas procesadas de cualquier origen
                    Movie movie = tmdbMovieLoaderService.loadMovieByTmdbId(tmdbId);
                    if (movie != null) {
                        imported++;
                        bulkImportedMovies.incrementAndGet();
                        updateBulkProgress(String.format("Procesada: %s. Total: %d/%d.",
                                movie.getTitle(), bulkImportedMovies.get(), bulkTargetMovies.get()));
                    } else {
                        errors++;
                        bulkErrorMovies.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors++;
                    bulkErrorMovies.incrementAndGet();
                    log.warn("Error importando película tmdbId={} en carga por objetivo: {}", tmdbId, e.getMessage());
                }
            }
        }

        long totalAfter = movieRepository.count();
        return new MovieLoadResult(type, safeTarget, imported, omitted, errors, pagesChecked, totalBefore, totalAfter);
    }

    private void updateBulkProgress(String message) {
        bulkLastMessage = message;
    }

    private JsonNode fetchMovieListPage(String type, int page) {
        return switch (type) {
            case "popular" -> tmdbClient.getPopular(page);
            case "topRated" -> tmdbClient.getTopRated(page);
            case "nowPlaying" -> tmdbClient.getNowPlaying(page);
            case "upcoming" -> tmdbClient.getUpcoming(page);
            case "discoverPopular" -> tmdbClient.discoverMovies(page, "popularity.desc");
            case "discoverVoteCount" -> tmdbClient.discoverMovies(page, "vote_count.desc");
            case "discoverReleaseDate" -> tmdbClient.discoverMovies(page, "primary_release_date.desc");
            case "discoverRevenue" -> tmdbClient.discoverMovies(page, "revenue.desc");
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
            if (!includeExisting && !shouldReloadPoster(movie.getPosterLocalPath())) {
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

    private boolean shouldReloadPoster(String posterLocalPath) {
        return posterLocalPath == null
                || posterLocalPath.isBlank()
                || posterLocalPath.startsWith("/images/")
                || posterLocalPath.startsWith("images/");
    }

    // Método utilitario para presets de carga masiva (switch mejorado)
    private String handlePreset(String presetName) {
        return switch (presetName.toLowerCase()) {
            case "quick" -> bulkLoadCombinedMoviesUntilTarget(200);
            case "medium" -> bulkLoadCombinedMoviesUntilTarget(1000);
            case "full" -> bulkLoadCombinedMoviesUntilTarget(4000);
            case "ultimate" -> bulkLoadCombinedMoviesUntilTarget(10000);
            case "categories" -> bulkLoadCombinedMoviesUntilTarget(1000);
            default -> "❌ Preset no válido: " + presetName;
        };
    }

    private int presetTarget(String presetName) {
        return switch (presetName.toLowerCase()) {
            case "quick" -> 200;
            case "medium", "categories" -> 1000;
            case "full" -> 4000;
            case "ultimate" -> 10000;
            default -> -1;
        };
    }

    private ResponseEntity<Map<String, Object>> startBulkLoadInBackground(
            String taskName,
            int targetMovies,
            Supplier<String> loader
    ) {
        int safeTarget = Math.min(Math.max(1, targetMovies), 10000);
        if (!bulkLoading.compareAndSet(false, true)) {
            return ResponseEntity.status(409).body(Map.of(
                    "success", false,
                    "message", "Ya hay una carga en progreso"
            ));
        }

        bulkCancelRequested.set(false);
        bulkTargetMovies.set(safeTarget);
        bulkImportedMovies.set(0);
        bulkOmittedMovies.set(0);
        bulkErrorMovies.set(0);
        bulkPagesChecked.set(0);
        bulkStartedAt.set(System.currentTimeMillis());
        bulkTaskName = taskName;
        bulkCurrentSource = "";
        bulkLastMessage = taskName + " iniciada en segundo plano. Puedes dejar esta página abierta y ver el progreso.";

        bulkLoaderExecutor.submit(() -> {
            try {
                String result = loader.get();
                bulkLastMessage = bulkCancelRequested.get()
                        ? "Carga cancelada. " + result
                        : result;
            } catch (Exception e) {
                log.error("❌ Error en carga masiva en segundo plano: {}", e.getMessage(), e);
                bulkLastMessage = "❌ Error en carga masiva: " + e.getMessage();
            } finally {
                bulkLoading.set(false);
                bulkCurrentSource = "";
            }
        });

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", bulkLastMessage,
                "targetMovies", safeTarget
        ));
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
        return useContentPreset("movies", presetName, auth);
    }

    @PostMapping("/admin/bulk-loader/{contentType}/preset/{presetName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> useContentPreset(
            @PathVariable String contentType,
            @PathVariable String presetName,
            Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));
        String safeContentType = EndpointSanitizer.contentType(contentType);
        String safePresetName = EndpointSanitizer.requiredText(presetName, "presetName", 20).toLowerCase();
        int target = presetTarget(safePresetName);
        if (target < 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Preset no válido: " + safePresetName
            ));
        }
        return startBulkLoadInBackground(
                "Preset " + safePresetName + " (" + safeContentType + ")",
                target,
                () -> bulkLoadContentUntilTarget(safeContentType, target)
        );
    }

    @GetMapping("/admin/bulk-loader/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBulkLoaderStatus(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));
        try {
            Map<String, Object> status = new HashMap<>();
            long movieCount = movieRepository.count();
            long seriesCount = tvShowRepository.count();
            long bookCount = bookRepository.count();
            status.put("success", true);
            status.put("movieCount", movieCount);
            status.put("seriesCount", seriesCount);
            status.put("bookCount", bookCount);
            status.put("currentMovieCount", movieCount);
            status.put("currentSeriesCount", seriesCount);
            status.put("currentBookCount", bookCount);
            status.put("isLoading", bulkLoading.get());
            status.put("taskName", bulkTaskName);
            status.put("currentSource", bulkCurrentSource);
            status.put("targetMovies", bulkTargetMovies.get());
            status.put("processedMovies", bulkImportedMovies.get());
            status.put("omittedMovies", bulkOmittedMovies.get());
            status.put("errorMovies", bulkErrorMovies.get());
            status.put("pagesChecked", bulkPagesChecked.get());
            status.put("currentPage", bulkImportedMovies.get());
            status.put("totalPages", bulkTargetMovies.get());
            status.put("progress", bulkTargetMovies.get() > 0
                    ? Math.min(100, (int) Math.round(bulkImportedMovies.get() * 100.0 / bulkTargetMovies.get()))
                    : 0);
            status.put("lastMessage", bulkLastMessage);
            status.put("startedAt", bulkStartedAt.get());
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
            @RequestParam(defaultValue = "movies") String contentType,
            Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));

        int safeTarget = EndpointSanitizer.size(targetMovies, 1000, 10000);
        String safeContentType = EndpointSanitizer.contentType(contentType);
        log.info("🚀 Programando carga personalizada: {} nuevos ({})", safeTarget, safeContentType);
        return startBulkLoadInBackground(
                "Carga personalizada (" + safeContentType + ")",
                safeTarget,
                () -> bulkLoadContentUntilTarget(safeContentType, safeTarget)
        );
    }

    @PostMapping("/admin/bulk-loader/start-categories")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startCategoriesBulkLoad(
            @RequestParam(defaultValue = "1000") int targetMovies,
            @RequestParam(defaultValue = "movies") String contentType,
            Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));

        int safeTarget = EndpointSanitizer.size(targetMovies, 1000, 10000);
        String safeContentType = EndpointSanitizer.contentType(contentType);
        log.info("🚀 Programando carga por categorías/fuentes: {} nuevos ({})", safeTarget, safeContentType);
        return startBulkLoadInBackground(
                "Carga por categorías (" + safeContentType + ")",
                safeTarget,
                () -> bulkLoadContentUntilTarget(safeContentType, safeTarget)
        );
    }

    @PostMapping("/admin/bulk-loader/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBulkLoad(Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Sin permisos de administrador"));

        Map<String, Object> response = new HashMap<>();
        try {
            log.info("🛑 Solicitud de cancelación de carga masiva");
            bulkCancelRequested.set(true);
            bulkLastMessage = "Cancelación solicitada. La carga se detendrá al terminar la película en curso.";
            response.put("success", true);
            response.put("message", bulkLastMessage);
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
            long seriesCount = tvShowRepository.count();
            long bookCount = bookRepository.count();
            addMovieStatsToModel(model, movieCount);
            model.addAttribute("currentSeriesCount", seriesCount);
            model.addAttribute("currentBookCount", bookCount);
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
            int movieTotal = tmdbMovieLoaderService.redownloadCastDirectorImages();
            ContentRepairService.RepairResult people = contentRepairService.repairPeopleImages(false);
            return "✅ Redescarga de imágenes de reparto y director completada: "
                    + (movieTotal + people.repaired()) + " imágenes. Omitidas: "
                    + people.skipped() + ". Errores: " + people.errors();
        } catch (Exception e) {
            log.error("❌ Error en redescarga de reparto/director: {}", e.getMessage());
            return "❌ Error en redescarga de reparto/director: " + e.getMessage();
        }
    }

    @PostMapping("/admin/repair-content")
    @ResponseBody
    public String repairContent(
            @RequestParam(defaultValue = "all") String contentType,
            @RequestParam(defaultValue = "false") boolean forceImages,
            Authentication auth) {
        String redirect = requireAdminOrRedirect(auth, null);
        if (redirect != null) return "❌ Sin permisos de administrador";
        try {
            String normalized = contentType.toLowerCase();
            if ("movies".equals(normalized) || "movie".equals(normalized)) {
                ContentRepairService.RepairResult movies = contentRepairService.repairMoviePosters(forceImages);
                return formatRepairResult("películas", movies);
            }
            if ("series".equals(normalized) || "tv".equals(normalized)) {
                return formatRepairResult("series", contentRepairService.repairSeriesPostersAndCast(forceImages));
            }
            if ("books".equals(normalized) || "book".equals(normalized) || "libros".equals(normalized)) {
                return formatRepairResult("libros", contentRepairService.repairBookCoversAndAuthors(forceImages));
            }
            if ("people".equals(normalized) || "reparto".equals(normalized)) {
                return formatRepairResult("reparto/directores", contentRepairService.repairPeopleImages(forceImages));
            }
            if ("all".equals(normalized)) {
                ContentRepairService.RepairResult movies = contentRepairService.repairMoviePosters(forceImages);
                ContentRepairService.RepairResult series = contentRepairService.repairSeriesPostersAndCast(forceImages);
                ContentRepairService.RepairResult books = contentRepairService.repairBookCoversAndAuthors(forceImages);
                ContentRepairService.RepairResult people = contentRepairService.repairPeopleImages(forceImages);
                return "✅ Reparación completa: "
                        + formatRepairResult("películas", movies) + " | "
                        + formatRepairResult("series", series) + " | "
                        + formatRepairResult("libros", books) + " | "
                        + formatRepairResult("reparto/directores", people);
            }
            return "❌ Tipo de contenido no válido: " + contentType;
        } catch (Exception e) {
            log.error("❌ Error reparando contenido {}: {}", contentType, e.getMessage(), e);
            return "❌ Error reparando contenido: " + e.getMessage();
        }
    }

    private String formatRepairResult(String label, ContentRepairService.RepairResult result) {
        return String.format("%s reparados: %d. Omitidos: %d. Errores: %d",
                label, result.repaired(), result.skipped(), result.errors());
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
