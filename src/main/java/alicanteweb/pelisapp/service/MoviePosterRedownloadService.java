package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la redescarga masiva de carátulas de películas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterRedownloadService {

    private final MovieRepository movieRepository;
    private final ImageStorageService imageStorageService;
    private final TMDBClient tmdbClient;

    /**
     * Este método está pensado para ser utilizado desde el panel de administración o scripts
     * para volver a descargar la carátula de una película concreta si ha fallado la descarga previa.
     * No se elimina aunque no tenga llamadas directas en el backend, ya que es útil para mantenimiento.
     */
    @Transactional
    public boolean redownloadMoviePoster(Movie movie) {
        if (movie.getPosterPath() == null || movie.getPosterPath().isBlank()) {
            log.debug("Película {} no tiene poster_path, saltando...", movie.getTitle());
            return false;
        }

        try {
            // Construir URL completa de TMDB. Algunos registros antiguos guardan la URL completa.
            String fullUrl = movie.getPosterPath().startsWith("http")
                    ? movie.getPosterPath()
                    : tmdbClient.buildImageUrl(movie.getPosterPath());
            if (fullUrl == null) {
                return false;
            }

            // Forzar redescarga de la imagen
            String filename = "movie_" + movie.getTmdbId();
            String localPath = imageStorageService.forceDownloadAndStoreImage(
                fullUrl, "posters", filename);

            if (localPath != null) {
                // Actualizar la ruta local en la base de datos
                movie.setPosterLocalPath(localPath);
                movieRepository.save(movie);
                return true;
            }

        } catch (Exception e) {
            log.error("Error redescargando poster de {}: {}", movie.getTitle(), e.getMessage());
        }

        return false;
    }
}
