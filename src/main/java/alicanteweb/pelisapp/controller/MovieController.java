package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.MovieDetailsDTO;
import alicanteweb.pelisapp.dto.MovieListDTO;
import alicanteweb.pelisapp.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {
    private final MovieService movieService;

    @Value("${app.movies.storage-path:./data/movies}")
    private String moviesStoragePath;

    @GetMapping("")
    public ResponseEntity<Page<MovieListDTO>> getAllMovies(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(EndpointSanitizer.page(page), EndpointSanitizer.size(size, 24, 60));
        Page<MovieListDTO> moviesPage = movieService.getAllMovies(pageable);
        return ResponseEntity.ok(moviesPage);
    }

    @GetMapping("/{id}/details")
    public MovieDetailsDTO getMovieDetails(@PathVariable Long id) {
        return movieService.getCombinedByMovieId(EndpointSanitizer.id(id, "movieId"));
    }

    @GetMapping("/tmdb/{tmdbId}/details")
    public MovieDetailsDTO getMovieDetailsByTmdbId(@PathVariable Long tmdbId) {
        return movieService.getCombinedByTmdbId(EndpointSanitizer.id(tmdbId, "tmdbId"));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<Map<String, Object>> getMovieFiles(@PathVariable Long id) {
        try {
            Long safeId = EndpointSanitizer.id(id, "movieId");
            Path movieDir = Paths.get(moviesStoragePath).resolve(safeId.toString());

            if (!Files.exists(movieDir)) {
                return ResponseEntity.ok(Map.of(
                    "movieId", safeId,
                    "files", List.of(),
                    "message", "No hay archivos disponibles para esta película"
                ));
            }

            List<Map<String, Object>> files;
            try (Stream<Path> stream = Files.list(movieDir)) {
                files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isVideoFile(path.getFileName().toString()))
                    .map(path -> {
                        try {
                            return Map.<String, Object>of(
                                "name", path.getFileName().toString(),
                                "size", Files.size(path),
                                "downloadUrl", "/movies/download/" + id + "/" + path.getFileName().toString(),
                                "streamUrl", "/movies/stream/" + id + "/" + path.getFileName()
                            );
                        } catch (IOException e) {
                            log.warn("Error obteniendo información del archivo: {}", path.getFileName());
                            return Map.<String, Object>of(
                                "name", path.getFileName().toString(),
                                "size", 0L,
                                "downloadUrl", "/movies/download/" + id + "/" + path.getFileName(),
                                "streamUrl", "/movies/stream/" + id + "/" + path.getFileName()
                            );
                        }
                    })
                    .toList();
            }

            return ResponseEntity.ok(Map.of(
                "movieId", safeId,
                "files", files,
                "totalFiles", files.size()
            ));

        } catch (IOException e) {
            log.error("Error listando archivos de película {}: {}", id, e.getMessage());
            return ResponseEntity.ok(Map.of(
                "movieId", id,
                "files", List.of(),
                "error", "Error accediendo a los archivos de la película"
            ));
        }
    }

    @GetMapping("/by-category")
    public ResponseEntity<Page<MovieListDTO>> getMoviesByCategory(@RequestParam String category,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(EndpointSanitizer.page(page), EndpointSanitizer.size(size, 24, 60));
        Page<MovieListDTO> moviesPage = movieService.getMoviesByCategory(
                EndpointSanitizer.requiredText(category, "category", 80), pageable);
        return ResponseEntity.ok(moviesPage);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieListDTO>> searchMovies(@RequestParam String query) {
        return ResponseEntity.ok(movieService.searchMovies(EndpointSanitizer.requiredText(query, "query", 100)));
    }

    @GetMapping("/top-rated-this-month")
    public ResponseEntity<MovieListDTO> getTopRatedThisMonth() {
        return movieService.getTopRatedThisMonth()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    private boolean isVideoFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".mp4") ||
               lowerName.endsWith(".mkv") ||
               lowerName.endsWith(".avi") ||
               lowerName.endsWith(".mov") ||
               lowerName.endsWith(".webm") ||
               lowerName.endsWith(".flv");
    }
}
