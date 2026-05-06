package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Actor;
import alicanteweb.pelisapp.entity.Director;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.repository.ActorRepository;
import alicanteweb.pelisapp.repository.DirectorRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class TMDBSeriesLoaderService {

    private final TMDBClient tmdbClient;
    private final TvShowRepository tvShowRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final ImageService imageService;
    private final String tmdbImageBaseUrl;

    public TMDBSeriesLoaderService(TMDBClient tmdbClient,
                                   TvShowRepository tvShowRepository,
                                   ActorRepository actorRepository,
                                   DirectorRepository directorRepository,
                                   ImageService imageService,
                                   @Value("${app.tmdb.image-base-url:https://image.tmdb.org/t/p/w500}") String tmdbImageBaseUrl) {
        this.tmdbClient = tmdbClient;
        this.tvShowRepository = tvShowRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.imageService = imageService;
        this.tmdbImageBaseUrl = tmdbImageBaseUrl;
    }

    @Transactional
    public TvShow importOrUpdateByTmdb(Long tmdbId) {
        JsonNode details = tmdbClient.getTvDetails(tmdbId);
        if (details == null || details.isNull()) {
            log.warn("TMDB returned no TV details for id {}", tmdbId);
            return null;
        }
        Optional<TvShow> existing = tvShowRepository.findByTmdbId(tmdbId);
        if (existing.isPresent()) {
            TvShow show = existing.get();
            mergeFromTmdb(show, details);
            return tvShowRepository.save(show);
        }
        TvShow show = new TvShow();
        show.setTmdbId(tmdbId);
        mergeFromTmdb(show, details);
        try {
            return tvShowRepository.save(show);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent insert detected for TV tmdbId {}", tmdbId);
            return tvShowRepository.findByTmdbId(tmdbId).orElseThrow(() -> ex);
        }
    }

    public int importPopularSeries(int pages) {
        int imported = 0;
        for (int p = 1; p <= pages; p++) {
            JsonNode resp = tmdbClient.getTvPopular(p);
            if (resp == null || !resp.has("results")) continue;
            for (JsonNode item : resp.path("results")) {
                if (!item.hasNonNull("id")) continue;
                Long id = item.path("id").asLong();
                try {
                    TvShow show = importOrUpdateByTmdb(id);
                    if (show != null) imported++;
                } catch (Exception e) {
                    log.warn("Error importing popular series tmdbId={}: {}", id, e.getMessage());
                }
            }
        }
        log.info("Imported {} popular series ({} pages)", imported, pages);
        return imported;
    }

    public int importTopRatedSeries(int pages) {
        int imported = 0;
        for (int p = 1; p <= pages; p++) {
            JsonNode resp = tmdbClient.getTvTopRated(p);
            if (resp == null || !resp.has("results")) continue;
            for (JsonNode item : resp.path("results")) {
                if (!item.hasNonNull("id")) continue;
                Long id = item.path("id").asLong();
                try {
                    TvShow show = importOrUpdateByTmdb(id);
                    if (show != null) imported++;
                } catch (Exception e) {
                    log.warn("Error importing top rated series tmdbId={}: {}", id, e.getMessage());
                }
            }
        }
        log.info("Imported {} top rated series ({} pages)", imported, pages);
        return imported;
    }

    private void mergeFromTmdb(TvShow show, JsonNode details) {
        if (show.getTmdbId() == null && details.hasNonNull("id")) {
            show.setTmdbId(details.path("id").asLong());
        }
        if ((show.getTitle() == null || show.getTitle().isBlank())) {
            String name = details.path("name").asText(null);
            if (name != null) show.setTitle(name);
        }
        if (show.getOriginalTitle() == null) {
            String orig = details.path("original_name").asText(null);
            if (orig != null) show.setOriginalTitle(orig);
        }
        if (show.getOverview() == null || show.getOverview().isBlank()) {
            String ov = details.path("overview").asText(null);
            if (ov != null) show.setOverview(ov);
        }
        if (show.getFirstAirDate() == null && details.hasNonNull("first_air_date")) {
            String fad = details.path("first_air_date").asText(null);
            if (fad != null && !fad.isBlank()) {
                try {
                    show.setFirstAirDate(LocalDate.parse(fad));
                } catch (DateTimeParseException e) {
                    log.debug("Invalid first_air_date format '{}' for tmdbId {}", fad, show.getTmdbId());
                }
            }
        }
        if (details.hasNonNull("number_of_seasons")) {
            show.setNumberOfSeasons(details.path("number_of_seasons").asInt());
        }
        if (details.hasNonNull("number_of_episodes")) {
            show.setNumberOfEpisodes(details.path("number_of_episodes").asInt());
        }
        if (show.getStatus() == null && details.hasNonNull("status")) {
            show.setStatus(details.path("status").asText(null));
        }
        if (show.getLanguage() == null && details.hasNonNull("original_language")) {
            show.setLanguage(details.path("original_language").asText(null));
        }
        // Poster — only attempt download if not already stored locally
        if (show.getPosterLocalPath() == null && details.hasNonNull("poster_path")) {
            String poster = details.path("poster_path").asText(null);
            if (poster != null && !poster.isBlank()) {
                String imageUrl = poster.startsWith("http") ? poster : tmdbImageBaseUrl + "/w500" + poster;
                String stored = imageService.downloadAndSave(imageUrl, "series_" + show.getTmdbId(), "series");
                if (stored != null) show.setPosterLocalPath(stored);
                else show.setPosterPath(poster); // store raw path so template can prepend CDN base
            }
        }
        // Backdrop
        if (show.getBackdropPath() == null && details.hasNonNull("backdrop_path")) {
            show.setBackdropPath(details.path("backdrop_path").asText(null));
        }
        // Genres as comma-separated string
        if (show.getGenres() == null && details.has("genres")) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode g : details.path("genres")) {
                String n = g.path("name").asText(null);
                if (n != null) { if (sb.length() > 0) sb.append(", "); sb.append(n); }
            }
            if (sb.length() > 0) show.setGenres(sb.toString());
        }
        // Credits
        if (details.has("credits")) {
            JsonNode credits = details.path("credits");
            if (credits.has("cast")) {
                Set<Actor> actors = new HashSet<>();
                int limit = 10, count = 0;
                for (JsonNode c : credits.path("cast")) {
                    if (count++ >= limit) break;
                    if (!c.path("id").canConvertToLong()) continue;
                    String name = c.path("name").asText(null);
                    if (name == null) continue;
                    long actorTmdbId = c.path("id").asLong();
                    Actor actor = actorRepository.findByTmdbId(actorTmdbId).orElseGet(() -> {
                        Actor a = new Actor();
                        a.setTmdbId(actorTmdbId);
                        a.setName(name);
                        return actorRepository.save(a);
                    });
                    actors.add(actor);
                }
                if (!actors.isEmpty()) show.setActors(actors);
            }
            if (credits.has("crew")) {
                Set<Director> directors = new HashSet<>();
                for (JsonNode cr : credits.path("crew")) {
                    String job = cr.path("job").asText(null);
                    if (!"Director".equalsIgnoreCase(job) && !"Series Director".equalsIgnoreCase(job)) continue;
                    if (!cr.path("id").canConvertToLong()) continue;
                    String name = cr.path("name").asText(null);
                    if (name == null) continue;
                    long dirTmdbId = cr.path("id").asLong();
                    Director d = directorRepository.findByTmdbId(dirTmdbId).orElseGet(() -> {
                        Director dir = new Director();
                        dir.setTmdbId(dirTmdbId);
                        dir.setName(name);
                        return directorRepository.save(dir);
                    });
                    directors.add(d);
                }
                if (!directors.isEmpty()) show.setDirectors(directors);
            }
        }
    }
}
