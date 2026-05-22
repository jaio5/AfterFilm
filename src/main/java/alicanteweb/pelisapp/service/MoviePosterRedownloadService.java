package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterRedownloadService {

    private final MovieRepository movieRepository;
    private final ImageStorageService imageStorageService;
    private final TMDBClient tmdbClient;

    @Transactional
    public boolean redownloadMoviePoster(Movie movie) {
        if (movie == null) {
            return false;
        }

        if (movie.getTmdbId() == null) {
            log.warn("Película {} no tiene tmdbId, saltando...", movie.getTitle());
            return false;
        }

        if (movie.getPosterPath() == null || movie.getPosterPath().isBlank()) {
            log.debug("Película {} no tiene poster_path, saltando...", movie.getTitle());
            return false;
        }

        try {
            String fullUrl = movie.getPosterPath().startsWith("http")
                    ? movie.getPosterPath()
                    : tmdbClient.buildImageUrl(movie.getPosterPath());

            if (fullUrl == null || fullUrl.isBlank()) {
                return false;
            }

            String filename = "movie_" + movie.getTmdbId();

            String localPath = imageStorageService.forceDownloadAndStoreImage(
                    fullUrl,
                    "posters",
                    filename
            );

            if (localPath == null || localPath.isBlank()) {
                return false;
            }

            movie.setPosterLocalPath(localPath);
            movieRepository.save(movie);

            return true;

        } catch (Exception e) {
            log.error("Error redescargando poster de {}", movie.getTitle(), e);
            return false;
        }
    }
}