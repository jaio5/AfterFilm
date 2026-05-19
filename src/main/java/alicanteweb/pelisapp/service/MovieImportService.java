package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Actor;
import alicanteweb.pelisapp.entity.CategoryEntity;
import alicanteweb.pelisapp.entity.Director;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.repository.ActorRepository;
import alicanteweb.pelisapp.repository.CategoryRepository;
import alicanteweb.pelisapp.repository.DirectorRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;

@Service
public class MovieImportService {

    private static final Logger log = LoggerFactory.getLogger(MovieImportService.class);

    private final TMDBClient tmdbClient;
    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final CategoryRepository categoryRepository;
    private final ImageService imageService;
    private final String tmdbImageBaseUrl;

    public MovieImportService(TMDBClient tmdbClient,
                              MovieRepository movieRepository,
                              ActorRepository actorRepository,
                              DirectorRepository directorRepository,
                              CategoryRepository categoryRepository,
                              ImageService imageService,
                              @Value("${app.tmdb.image-base-url:https://image.tmdb.org/t/p/w500}") String tmdbImageBaseUrl) {
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.categoryRepository = categoryRepository;
        this.imageService = imageService;
        this.tmdbImageBaseUrl = tmdbImageBaseUrl;
    }

    /**
     * Importa la película por tmdbId si no existe. Retorna la entidad persistida.
     * Es seguro frente a concurrencia: intenta crear y si falla por unique constraint reintenta obtener.
     */
    @Transactional
    public Movie importIfMissing(Long tmdbId) {
        Optional<Movie> existing = movieRepository.findByTmdbId(tmdbId);
        if (existing.isPresent()) return existing.get();

        JsonNode details = tmdbClient.getMovieDetails(tmdbId);
        if (details == null || details.isNull()) {
            log.warn("TMDB returned no details for id {}", tmdbId);
            return null;
        }

        Movie m = new Movie();
        m.setTmdbId(tmdbId);

        mergeFromTmdb(m, details);

        // try save; if another thread saved concurrently, catch and re-query
        try {
            return movieRepository.save(m);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent insert detected for tmdbId {}, reloading existing record", tmdbId);
            return movieRepository.findByTmdbId(tmdbId).orElseThrow(() -> ex);
        }
    }

    /**
     * Importa o actualiza la película desde TMDB. Si la película existe, intenta fusionar campos y guardar.
     * Retorna la entidad persistida (nueva o actualizada) o null si TMDB no devolvió datos.
     */
    @Transactional
    public Movie importOrUpdateByTmdb(Long tmdbId) {
        JsonNode details = tmdbClient.getMovieDetails(tmdbId);
        if (details == null || details.isNull()) {
            log.warn("TMDB returned no details for id {}", tmdbId);
            return null;
        }

        // Si ya existe, fusionar y guardar
        Optional<Movie> existing = movieRepository.findByTmdbId(tmdbId);
        if (existing.isPresent()) {
            Movie movie = existing.get();
            boolean changed = mergeFromTmdb(movie, details);
            if (changed) {
                movie = movieRepository.save(movie);
            }
            return movie;
        }

        // No existe: crear uno nuevo (reutiliza importIfMissing logic)
        return importIfMissing(tmdbId);
    }

    // Mapper similar al existente en MovieService pero autónomo para la importación
    private boolean mergeFromTmdb(Movie movie, JsonNode details) {
        boolean changed = false;

        if (movie.getTmdbId() == null && details.hasNonNull("id")) {
            movie.setTmdbId(details.path("id").asLong());
            changed = true;
        }

        if ((movie.getTitle() == null || movie.getTitle().isBlank()) && details.hasNonNull("title")) {
            movie.setTitle(details.path("title").asText());
            changed = true;
        }

        if ((movie.getDescription() == null || movie.getDescription().isBlank()) && details.hasNonNull("overview")) {
            String overview = details.path("overview").asText(null);
            if (overview != null) {
                movie.setDescription(overview);
                changed = true;
            }
        }

        if (movie.getReleaseDate() == null && details.hasNonNull("release_date")) {
            String rd = details.path("release_date").asText(null);
            if (rd != null && !rd.isBlank()) {
                try {
                    movie.setReleaseDate(LocalDate.parse(rd));
                    changed = true;
                } catch (DateTimeParseException e) {
                    log.debug("Invalid release_date format '{}' for tmdbId {}", rd, movie.getTmdbId());
                }
            }
        }

        if ((movie.getRuntimeMinutes() == null || movie.getRuntimeMinutes() == 0) && details.hasNonNull("runtime")) {
            if (details.path("runtime").canConvertToInt()) {
                movie.setRuntimeMinutes(details.path("runtime").asInt());
                changed = true;
            }
        }

        // Poster: preferimos descargar y almacenar localmente si ImageService disponible
        if ((movie.getPosterPath() == null || movie.getPosterPath().isBlank()) && details.hasNonNull("poster_path")) {
            String poster = details.path("poster_path").asText(null);
            if (poster != null && !poster.isBlank()) {
                // ImageService.downloadAndSave espera URL completa. TMDBClient normaliza base, tamaño y barras.
                String imageUrl = poster.startsWith("http") ? poster : tmdbClient.buildImageUrl(poster);

                String stored = imageService.downloadAndSave(imageUrl, "movie_" + movie.getTmdbId(), "posters");
                if (stored != null) {
                    movie.setPosterLocalPath(stored);
                } else {
                    movie.setPosterPath(imageUrl);
                }
                changed = true;
            }
        }

        // Credits: cast and crew
        if (details.has("credits")) {
            JsonNode credits = details.path("credits");
            // Actors (cast) - store first 10
            if (credits.has("cast")) {
                Set<Actor> actors = new HashSet<>();
                int limit = 10;
                int count = 0;
                for (JsonNode c : credits.path("cast")) {
                    if (count++ >= limit) break;
                    String name = c.path("name").asText(null);
                    if (name == null) continue;
                    Actor actor = findOrCreateActor(name, c.path("id").canConvertToLong() ? c.path("id").asLong() : null);
                    actors.add(actor);
                }
                if (!actors.isEmpty()) {
                    movie.setActors(actors);
                    changed = true;
                }
            }

            // Directors - crew role = "Director"
            if (credits.has("crew")) {
                Set<Director> directors = new HashSet<>();
                for (JsonNode cr : credits.path("crew")) {
                    String job = cr.path("job").asText(null);
                    if (job != null && job.equalsIgnoreCase("Director")) {
                        String name = cr.path("name").asText(null);
                        if (name == null) continue;
                        Director d = findOrCreateDirector(name, cr.path("id").canConvertToLong() ? cr.path("id").asLong() : null);
                        directors.add(d);
                    }
                }
                if (!directors.isEmpty()) {
                    movie.setDirectors(directors);
                    changed = true;
                }
            }
        }

        // Genres -> CategoryEntity
        if (details.has("genres")) {
            Set<CategoryEntity> cats = new HashSet<>();
            for (JsonNode g : details.path("genres")) {
                String name = g.path("name").asText(null);
                if (name == null) continue;
                CategoryEntity ce = categoryRepository.findByName(name).orElseGet(() -> {
                    CategoryEntity nce = new CategoryEntity();
                    nce.setName(name);
                    return categoryRepository.save(nce);
                });
                cats.add(ce);
            }
            if (!cats.isEmpty()) {
                movie.setCategories(cats);
                changed = true;
            }
        }

        return changed;
    }

    private Actor findOrCreateActor(String name, Long tmdbId) {
        if (tmdbId != null) {
            Optional<Actor> existing = actorRepository.findByTmdbId(tmdbId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Actor actor = new Actor();
        actor.setName(name);
        actor.setTmdbId(tmdbId);
        try {
            return actorRepository.save(actor);
        } catch (DataIntegrityViolationException ex) {
            if (tmdbId != null) {
                log.info("Actor concurrente detectado para tmdbId {}, reutilizando registro existente", tmdbId);
                return actorRepository.findByTmdbId(tmdbId).orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private Director findOrCreateDirector(String name, Long tmdbId) {
        if (tmdbId != null) {
            Optional<Director> existing = directorRepository.findByTmdbId(tmdbId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Director director = new Director();
        director.setName(name);
        director.setTmdbId(tmdbId);
        try {
            return directorRepository.save(director);
        } catch (DataIntegrityViolationException ex) {
            if (tmdbId != null) {
                log.info("Director concurrente detectado para tmdbId {}, reutilizando registro existente", tmdbId);
                return directorRepository.findByTmdbId(tmdbId).orElseThrow(() -> ex);
            }
            throw ex;
        }
    }
}
