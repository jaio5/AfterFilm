package alicanteweb.pelisapp.tmdb;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class TMDBClient {
    private static final Logger log = LoggerFactory.getLogger(TMDBClient.class);

    private final WebClient webClient;

    @Value("${app.tmdb.api-key:}")
    private String apiKey;

    @Value("${app.tmdb.bearer-token:}")
    private String bearerToken;

    // cached configuration
    private volatile String imagesBaseUrl;
    private volatile String posterSize; // e.g. w500

    public TMDBClient(WebClient tmdbWebClient) {
        this.webClient = tmdbWebClient;
    }

    private synchronized void ensureConfigurationLoaded() {
        if (imagesBaseUrl != null && posterSize != null) return;
        log.debug("Loading TMDB configuration (ensureConfigurationLoaded)");
        try {
            JsonNode cfg = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/configuration");
                        // Añadir API key como query param si no hay bearer token
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            if (cfg != null && cfg.has("images")) {
                JsonNode images = cfg.path("images");
                imagesBaseUrl = images.path("secure_base_url").asText(images.path("base_url").asText("https://image.tmdb.org/t/p"));
                // pick poster size prefer w500
                posterSize = "w500";
                if (images.has("poster_sizes")) {
                    for (JsonNode s : images.path("poster_sizes")) {
                        String st = s.asText();
                        if ("w500".equals(st)) { posterSize = "w500"; break; }
                    }
                    if (posterSize == null) posterSize = images.path("poster_sizes").get(0).asText("w500");
                }
                log.debug("TMDB configuration loaded: imagesBaseUrl={}, posterSize={}", imagesBaseUrl, posterSize);
            } else {
                log.debug("TMDB configuration endpoint returned null or missing 'images'");
            }
        } catch (WebClientResponseException we) {
            log.warn("TMDB configuration failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB configuration failed: {}", e.getMessage());
        }
        if (imagesBaseUrl == null) imagesBaseUrl = "https://image.tmdb.org/t/p";
        if (posterSize == null) posterSize = "w500";
    }

    public JsonNode getPopular(int page) {
        log.debug("Requesting TMDB popular page={} using {}", page, (bearerToken != null && !bearerToken.isBlank()) ? "Bearer token" : (apiKey != null && !apiKey.isBlank() ? "API key" : "no auth"));
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/movie/popular")
                                .queryParam("page", page)
                                .queryParam("language", "es-ES");

                        // Añadir API key como query param si no hay bearer token
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            if (resp != null && resp.has("results") && resp.path("results").isArray()) {
                int count = resp.path("results").size();
                log.debug("TMDB popular page={} returned {} results", page, count);
            } else {
                log.debug("TMDB popular page={} returned null or no results array", page);
            }
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB getPopular failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getPopular failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene películas trending desde TMDB
     */
    public JsonNode getTrending(String mediaType, String timeWindow) {
        log.debug("Getting TMDB trending {} for {}", mediaType, timeWindow);
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/trending/{media_type}/{time_window}");

                        // Añadir API key como query param si no hay bearer token
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build(mediaType, timeWindow);
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            log.debug("TMDB trending {} results: {} items", mediaType,
                    resp != null ? resp.path("results").size() : 0);
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB getTrending failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getTrending failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene películas top rated desde TMDB
     */
    public JsonNode getTopRated(int page) {
        log.debug("Getting TMDB top rated movies page {}", page);
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/movie/top_rated")
                                .queryParam("page", page)
                                .queryParam("language", "es-ES");

                        // Añadir API key como query param si no hay bearer token
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            log.debug("TMDB top rated page {} results: {} movies",
                    page, resp != null ? resp.path("results").size() : 0);
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB getTopRated failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getTopRated failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode getNowPlaying(int page) {
        return getMovieList("/movie/now_playing", page, "now playing");
    }

    public JsonNode getUpcoming(int page) {
        return getMovieList("/movie/upcoming", page, "upcoming");
    }

    public JsonNode discoverMovies(int page, String sortBy) {
        log.debug("Discovering TMDB movies page {} sorted by {}", page, sortBy);
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/discover/movie")
                                .queryParam("page", page)
                                .queryParam("language", "es-ES")
                                .queryParam("sort_by", sortBy)
                                .queryParam("include_adult", false)
                                .queryParam("include_video", false);

                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            log.debug("TMDB discover {} page {} results: {} movies",
                    sortBy, page, resp != null ? resp.path("results").size() : 0);
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB discoverMovies failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB discoverMovies failed: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode getMovieList(String path, int page, String label) {
        log.debug("Getting TMDB {} movies page {}", label, page);
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path)
                                .queryParam("page", page)
                                .queryParam("language", "es-ES");

                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            log.debug("TMDB {} page {} results: {} movies",
                    label, page, resp != null ? resp.path("results").size() : 0);
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB {} failed: status={} body={}", label, we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB {} failed: {}", label, e.getMessage());
        }
        return null;
    }

    public JsonNode getMovieDetails(long tmdbId) {
        log.debug("Requesting TMDB movie details for tmdbId={} using {}", tmdbId, (bearerToken != null && !bearerToken.isBlank()) ? "Bearer token" : (apiKey != null && !apiKey.isBlank() ? "API key" : "no auth"));
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/movie/{id}")
                                .queryParam("language", "es-ES")
                                .queryParam("append_to_response", "credits");

                        // Añadir API key como query param si no hay bearer token
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build(tmdbId);
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            if (resp == null) {
                log.debug("TMDB movie details for tmdbId={} returned null", tmdbId);
            } else {
                log.debug("TMDB movie details for tmdbId={} received", tmdbId);
            }
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB getMovieDetails failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getMovieDetails failed: {}", e.getMessage());
        }
        return null;
    }

    /** Devuelve la URL completa del poster con el tamaño elegido (cached) */
    public String buildImageUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) return null;
        ensureConfigurationLoaded();
        String clean = posterPath.startsWith("/") ? posterPath.substring(1) : posterPath;
        String base = (imagesBaseUrl != null && !imagesBaseUrl.isBlank()) ? imagesBaseUrl : "https://image.tmdb.org/t/p";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String size = (posterSize != null && !posterSize.isBlank()) ? posterSize : "w500";
        while (size.startsWith("/")) {
            size = size.substring(1);
        }
        while (size.endsWith("/")) {
            size = size.substring(0, size.length() - 1);
        }
        String full = base + "/" + size + "/" + clean;
        log.debug("buildImageUrl posterPath={} -> {}", posterPath, full);
        return full;
    }

    /**
     * Busca películas por título en TMDB.
     * Este método está disponible para futuras integraciones de búsqueda avanzada.
     */
    public JsonNode searchMovie(String query) {
        log.debug("Searching TMDB for movie: {}", query);
        try {
            JsonNode resp = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/search/movie")
                                .queryParam("query", query)
                                .queryParam("language", "es-ES");

                        // Añadir API key como query param si no hay bearer token
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            log.debug("TMDB search results for '{}': {} movies found",
                    query, resp != null ? resp.path("total_results").asInt() : 0);
            return resp;
        } catch (WebClientResponseException we) {
            log.warn("TMDB searchMovie failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB searchMovie failed: {}", e.getMessage());
        }
        return null;
    }

    // ============= TV SHOW METHODS =============

    public JsonNode getTvPopular(int page) {
        log.debug("Requesting TMDB TV popular page={}", page);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/tv/popular")
                                .queryParam("page", page)
                                .queryParam("language", "es-ES");
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) h.setBearerAuth(bearerToken);
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException we) {
            log.warn("TMDB getTvPopular failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getTvPopular failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode getTvTopRated(int page) {
        log.debug("Requesting TMDB TV top rated page={}", page);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/tv/top_rated")
                                .queryParam("page", page)
                                .queryParam("language", "es-ES");
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) h.setBearerAuth(bearerToken);
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException we) {
            log.warn("TMDB getTvTopRated failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getTvTopRated failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode getTvDetails(long tmdbId) {
        log.debug("Requesting TMDB TV details for tmdbId={}", tmdbId);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/tv/{id}")
                                .queryParam("language", "es-ES")
                                .queryParam("append_to_response", "credits");
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build(tmdbId);
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) h.setBearerAuth(bearerToken);
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException we) {
            log.warn("TMDB getTvDetails failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB getTvDetails failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode searchTv(String query, int page) {
        log.debug("Searching TMDB TV for: {}", query);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/search/tv")
                                .queryParam("query", query)
                                .queryParam("page", page)
                                .queryParam("language", "es-ES");
                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }
                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) h.setBearerAuth(bearerToken);
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException we) {
            log.warn("TMDB searchTv failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("TMDB searchTv failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene información de paginación para películas populares.
     * Método de utilidad para administración y diagnóstico.
     */
    public JsonNode getPopularInfo() {
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/movie/popular")
                                .queryParam("page", 1)
                                .queryParam("language", "es-ES");

                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.warn("Error getting popular info: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el número total de páginas disponibles para películas top rated.
     * Método de utilidad para administración y diagnóstico.
     */
    public JsonNode getTopRatedInfo() {
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/movie/top_rated")
                                .queryParam("page", 1)
                                .queryParam("language", "es-ES");

                        if ((bearerToken == null || bearerToken.isBlank()) && apiKey != null && !apiKey.isBlank()) {
                            uriBuilder.queryParam("api_key", apiKey);
                        }

                        return uriBuilder.build();
                    })
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.warn("Error getting top rated info: {}", e.getMessage());
            return null;
        }
    }
}
