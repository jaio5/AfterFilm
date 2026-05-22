package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.service.ImageStorageService;
import alicanteweb.pelisapp.service.image.SupabaseImageStorage;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SupabaseImageController {

    private static final Pattern POSTER_OBJECT_PATTERN = Pattern.compile(
            "(?:^|/)(posters|series)/(movie|series)_(\\d+)(?:_[^/]+)?\\.(?:jpe?g|png|webp)$",
            Pattern.CASE_INSENSITIVE);

    private final SupabaseImageStorage supabaseImageStorage;
    private final ImageStorageService imageStorageService;
    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final TMDBClient tmdbClient;

    @GetMapping("/supabase-images/**")
    public ResponseEntity<byte[]> serveSupabaseImage(HttpServletRequest request) {
        String path = Optional.ofNullable((String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
                .orElse("");
        String objectKey = supabaseImageStorage.objectKeyFromRequestPath(path.replaceFirst("^/supabase-images/?", ""));
        if (objectKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Optional<SupabaseImageStorage.StoredImage> image = supabaseImageStorage.fetch(objectKey);
            if (image.isEmpty()) {
                image = repairMissingPosterAndFetch(objectKey);
                if (image.isEmpty()) {
                    log.warn("Imagen no encontrada en Supabase Storage: {}", objectKey);
                    return ResponseEntity.notFound().build();
                }
            }
            SupabaseImageStorage.StoredImage storedImage = image.get();
            MediaType mediaType = MediaType.parseMediaType(storedImage.contentType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(storedImage.content());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Petición interrumpida sirviendo imagen desde Supabase: {}", objectKey);
            return ResponseEntity.status(503).build();
        } catch (Exception e) {
            log.error("No se pudo servir imagen desde Supabase {}: {} - {}", objectKey, e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private Optional<SupabaseImageStorage.StoredImage> repairMissingPosterAndFetch(String objectKey) {
        Matcher matcher = POSTER_OBJECT_PATTERN.matcher(objectKey);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String requestedSubfolder = matcher.group(1).toLowerCase();
        String mediaType = matcher.group(2).toLowerCase();
        Long tmdbId = Long.valueOf(matcher.group(3));

        try {
            String storedPath = switch (mediaType) {
                case "movie" -> repairMoviePoster(tmdbId);
                case "series" -> repairSeriesPoster(tmdbId, requestedSubfolder);
                default -> null;
            };
            if (storedPath == null || storedPath.isBlank()) {
                return Optional.empty();
            }

            Optional<SupabaseImageStorage.StoredImage> repaired = supabaseImageStorage.fetch(objectKey);
            if (repaired.isPresent()) {
                return repaired;
            }
            return fetchStoredPath(storedPath);
        } catch (Exception e) {
            log.warn("No se pudo reparar imagen faltante {}: {}", objectKey, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<SupabaseImageStorage.StoredImage> fetchStoredPath(String storedPath) throws Exception {
        String objectKey = storedPath;
        if (storedPath.startsWith("/supabase-images/")) {
            objectKey = storedPath.substring("/supabase-images/".length());
        } else if (storedPath.startsWith("supabase-images/")) {
            objectKey = storedPath.substring("supabase-images/".length());
        }
        return supabaseImageStorage.fetch(objectKey);
    }

    private String repairMoviePoster(Long tmdbId) {
        Optional<Movie> found = movieRepository.findByTmdbId(tmdbId);
        if (found.isEmpty()) {
            return null;
        }
        Movie movie = found.get();
        String posterPath = resolveMoviePosterPath(movie);
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }

        String storedPath = imageStorageService.forceDownloadAndStoreImage(
                imageUrlForPosterPath(posterPath),
                "posters",
                "movie_" + tmdbId);
        if (storedPath != null && !storedPath.isBlank()) {
            movie.setPosterPath(posterPath);
            movie.setPosterLocalPath(storedPath);
            movieRepository.save(movie);
        }
        return storedPath;
    }

    private String repairSeriesPoster(Long tmdbId, String requestedSubfolder) {
        Optional<TvShow> found = tvShowRepository.findByTmdbId(tmdbId);
        if (found.isEmpty()) {
            return null;
        }
        TvShow series = found.get();
        String posterPath = resolveSeriesPosterPath(series);
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }

        String subfolder = "posters".equals(requestedSubfolder) ? "posters" : "series";
        String storedPath = imageStorageService.forceDownloadAndStoreImage(
                imageUrlForPosterPath(posterPath),
                subfolder,
                "series_" + tmdbId);
        if (storedPath != null && !storedPath.isBlank()) {
            series.setPosterPath(posterPath);
            series.setPosterLocalPath(storedPath);
            tvShowRepository.save(series);
        }
        return storedPath;
    }

    private String resolveMoviePosterPath(Movie movie) {
        if (movie.getPosterPath() != null && !movie.getPosterPath().isBlank()) {
            return movie.getPosterPath();
        }
        JsonNode details = tmdbClient.getMovieDetails(movie.getTmdbId());
        return details == null ? null : details.path("poster_path").asText(null);
    }

    private String resolveSeriesPosterPath(TvShow series) {
        if (series.getPosterPath() != null && !series.getPosterPath().isBlank()) {
            return series.getPosterPath();
        }
        JsonNode details = tmdbClient.getTvDetails(series.getTmdbId());
        return details == null ? null : details.path("poster_path").asText(null);
    }

    private String imageUrlForPosterPath(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return posterPath.startsWith("http://") || posterPath.startsWith("https://")
                ? posterPath
                : tmdbClient.buildImageUrl(posterPath);
    }
}
