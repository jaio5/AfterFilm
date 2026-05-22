package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.CastDTO;
import alicanteweb.pelisapp.dto.CrewDTO;
import alicanteweb.pelisapp.dto.MovieDetailsDTO;
import alicanteweb.pelisapp.dto.MovieListDTO;
import alicanteweb.pelisapp.entity.Actor;
import alicanteweb.pelisapp.entity.CategoryEntity;
import alicanteweb.pelisapp.entity.Director;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de películas con funcionalidad básica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final TMDBClient tmdbClient;
    private final MovieRepository movieRepository;
    private final MovieImportService movieImportService;
    private final ImageUrlService imageUrlService;

    public MovieDetailsDTO getCombinedByMovieId(Long id) {
        Optional<Movie> opt = movieRepository.findByIdWithCastAndDirectors(id);
        if (opt.isEmpty()) return null;

        Movie movie = opt.get();
        log.debug("Obteniendo detalles combinados para película: {} (ID: {}, TMDB ID: {})",
                 movie.getTitle(), movie.getId(), movie.getTmdbId());

        MovieDetailsDTO dto = createBaseMovieDTO(movie);

        // Cargar reparto y directores
        List<CastDTO> castMembers = loadCastMembers(movie);
        List<CrewDTO> directors = loadDirectors(movie);

        // Si no hay datos en BD, usar fallback con TMDB API
        if (castMembers.isEmpty() || directors.isEmpty()) {
            loadMissingDataFromTMDB(movie, castMembers, directors);
        }

        configureDTOWithCastData(dto, castMembers, directors);

        log.info("Detalles preparados para '{}': {} actores, {} directores",
                 movie.getTitle(), castMembers.size(), directors.size());

        return dto;
    }

    private MovieDetailsDTO createBaseMovieDTO(Movie movie) {
        MovieDetailsDTO dto = new MovieDetailsDTO();
        dto.setId(movie.getId());
        dto.setTmdbId(movie.getTmdbId());
        dto.setTitle(movie.getTitle());
        dto.setOverview(movie.getDescription());
        dto.setPosterPath(imageUrlService.moviePosterUrl(movie, "w500"));
        dto.setReleaseDate(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null);
        return dto;
    }

    private List<CastDTO> loadCastMembers(Movie movie) {
        List<CastDTO> castMembers = new ArrayList<>();

        if (!movie.getActors().isEmpty()) {
            log.debug("Usando actores desde BD: {} actores encontrados", movie.getActors().size());
            for (Actor actor : movie.getActors()) {
                CastDTO castDto = createCastDTO(actor);
                castMembers.add(castDto);
            }
            log.info("✅ Cargados {} actores desde BD para '{}'", castMembers.size(), movie.getTitle());
        }

        return castMembers;
    }

    private CastDTO createCastDTO(Actor actor) {
        CastDTO castDto = new CastDTO();
        castDto.setTmdbId(actor.getTmdbId());
        castDto.setName(actor.getName());
        castDto.setCharacter("Reparto Principal"); // Mejorable en futuras versiones
        castDto.setProfilePath(actor.getProfilePath());
        castDto.setProfileLocalPath(actor.getProfileLocalPath());
        castDto.setProfileUrl(getImageUrl(actor.getProfileLocalPath(), actor.getProfilePath()));
        return castDto;
    }

    private List<CrewDTO> loadDirectors(Movie movie) {
        List<CrewDTO> directors = new ArrayList<>();

        if (!movie.getDirectors().isEmpty()) {
            log.debug("Usando directores desde BD: {} directores encontrados", movie.getDirectors().size());
            for (Director director : movie.getDirectors()) {
                CrewDTO directorDto = createDirectorDTO(director);
                directors.add(directorDto);
            }
            log.info("✅ Cargados {} directores desde BD para '{}'", directors.size(), movie.getTitle());
        }

        return directors;
    }

    private CrewDTO createDirectorDTO(Director director) {
        CrewDTO directorDto = new CrewDTO();
        directorDto.setTmdbId(director.getTmdbId());
        directorDto.setName(director.getName());
        directorDto.setJob("Director");
        directorDto.setDepartment("Directing");
        directorDto.setProfilePath(director.getProfilePath());
        directorDto.setProfileLocalPath(director.getProfileLocalPath());
        directorDto.setProfileUrl(getImageUrl(director.getProfileLocalPath(), director.getProfilePath()));
        return directorDto;
    }

    private void loadMissingDataFromTMDB(Movie movie, List<CastDTO> castMembers, List<CrewDTO> directors) {
        log.debug("Datos incompletos en BD, intentando fallback con TMDB API...");

        JsonNode details = getTMDBMovieDetails(movie.getTmdbId());
        if (details != null && details.has("credits")) {
            JsonNode credits = details.path("credits");

            if (castMembers.isEmpty() && credits.has("cast")) {
                loadCastFromTMDB(credits.path("cast"), castMembers);
            }

            if (directors.isEmpty() && credits.has("crew")) {
                loadDirectorsFromTMDB(credits.path("crew"), directors);
            }
        }
    }

    private JsonNode getTMDBMovieDetails(Long tmdbId) {
        if (tmdbId == null) return null;

        try {
            return tmdbClient.getMovieDetails(tmdbId);
        } catch (Exception e) {
            log.warn("Error obteniendo detalles de TMDB para película {}: {}", tmdbId, e.getMessage());
            return null;
        }
    }

    private void loadCastFromTMDB(JsonNode castNode, List<CastDTO> castMembers) {
        log.debug("Cargando actores desde TMDB API como fallback");
        int limit = 10;
        int count = 0;

        for (JsonNode c : castNode) {
            if (count++ >= limit) break;

            CastDTO castDto = new CastDTO();
            castDto.setTmdbId(c.path("id").canConvertToLong() ? c.path("id").asLong() : null);
            castDto.setName(c.path("name").asText());
            castDto.setCharacter(c.path("character").asText());
            castDto.setProfilePath(c.path("profile_path").asText(null));
            castDto.setProfileUrl(getImageUrl(null, castDto.getProfilePath()));
            castMembers.add(castDto);
        }
    }

    private void loadDirectorsFromTMDB(JsonNode crewNode, List<CrewDTO> directors) {
        log.debug("Cargando directores desde TMDB API como fallback");

        for (JsonNode c : crewNode) {
            String job = c.path("job").asText();
            if ("Director".equalsIgnoreCase(job)) {
                CrewDTO directorDto = new CrewDTO();
                directorDto.setTmdbId(c.path("id").canConvertToLong() ? c.path("id").asLong() : null);
                directorDto.setName(c.path("name").asText());
                directorDto.setJob(job);
                directorDto.setDepartment(c.path("department").asText());
                directorDto.setProfilePath(c.path("profile_path").asText(null));
                directorDto.setProfileUrl(getImageUrl(null, directorDto.getProfilePath()));
                directors.add(directorDto);
            }
        }
    }

    private void configureDTOWithCastData(MovieDetailsDTO dto, List<CastDTO> castMembers, List<CrewDTO> directors) {
        dto.setCastMembers(castMembers);
        dto.setDirectors(directors);

        // Lista simple para compatibilidad
        List<String> cast = castMembers.stream()
                .limit(6)
                .map(CastDTO::getName)
                .collect(Collectors.toList());
        dto.setCast(cast);
    }

    public MovieDetailsDTO getCombinedByTmdbId(Long tmdbId) {
        // Delegar en MovieImportService: importOrUpdateByTmdb devuelve la entidad persistida
        Movie m = movieImportService.importOrUpdateByTmdb(tmdbId);
        if (m == null) return null;
        return getCombinedByMovieId(m.getId());
    }

    /**
     * Determina qué URL usar para mostrar la imagen
     */
    private String getImageUrl(String localPath, String remotePath) {
        return imageUrlService.posterUrl(localPath, remotePath, "w500");
    }


    @Transactional(readOnly = true)
    public Page<MovieListDTO> getAllMovies(Pageable pageable) {
        Page<Movie> moviePage = movieRepository.findAll(pageable);
        List<MovieListDTO> movieDTOs = mapWithRatingStats(moviePage.getContent());
        return new PageImpl<>(movieDTOs, pageable, moviePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<MovieListDTO> getMoviesByCategory(String category, Pageable pageable) {
        Page<Movie> moviePage = movieRepository.findByCategories_Name(category, pageable);
        List<MovieListDTO> movieDTOs = mapWithRatingStats(moviePage.getContent());
        return new PageImpl<>(movieDTOs, pageable, moviePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<MovieListDTO> searchMovies(String query) {
        return mapWithRatingStats(
            movieRepository.findByTitleContainingIgnoreCase(query, PageRequest.of(0, 50)).getContent()
        );
    }

    private List<MovieListDTO> mapWithRatingStats(List<Movie> movies) {
        if (movies.isEmpty()) return List.of();
        List<Long> ids = movies.stream().map(Movie::getId).toList();
        Map<Long, double[]> ratingMap = RatingStatsHelper.buildRatingMap(movieRepository.findRatingStatsByIds(ids));
        return movies.stream().map(m -> toMovieListDTO(m, ratingMap.get(m.getId()))).toList();
    }

    private MovieListDTO toMovieListDTO(Movie movie, double[] stats) {
        MovieListDTO dto = new MovieListDTO();
        dto.setId(movie.getId());
        dto.setTmdbId(movie.getTmdbId());
        dto.setTitle(movie.getTitle());
        dto.setDescription(movie.getDescription());
        String posterUrl = imageUrlService.moviePosterUrl(movie, "w500");
        dto.setPosterPath(posterUrl);
        dto.setPosterLocalPath(posterUrl);
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setRuntimeMinutes(movie.getRuntimeMinutes());
        dto.setCategories(movie.getCategories().stream().map(CategoryEntity::getName).toList());
        if (stats != null && stats[1] > 0) {
            dto.setReviewCount((int) stats[1]);
            dto.setAvgRating(stats[0]);
        } else {
            dto.setReviewCount(0);
        }
        return dto;
    }

    private MovieListDTO toMovieListDTO(Movie movie) {
        return toMovieListDTO(movie, null);
    }

    @Transactional(readOnly = true)
    public Optional<MovieListDTO> getTopRatedThisMonth() {
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1)
            .atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Movie> movies = movieRepository.findTopRatedThisMonth(
            startOfMonth, PageRequest.of(0, 1));
        if (!movies.isEmpty()) {
            return Optional.of(mapWithRatingStats(movies).get(0));
        }
        Page<Movie> page = movieRepository.findAll(PageRequest.of(0, 1));
        return page.getContent().isEmpty()
            ? Optional.empty()
            : Optional.of(mapWithRatingStats(page.getContent()).get(0));
    }
}
