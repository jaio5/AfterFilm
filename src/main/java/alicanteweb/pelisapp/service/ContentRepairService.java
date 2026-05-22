package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.constants.AppConstants;
import alicanteweb.pelisapp.entity.Actor;
import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.entity.Director;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.repository.ActorRepository;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.repository.DirectorRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.tmdb.TMDBClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentRepairService {

    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final BookRepository bookRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final MoviePosterRedownloadService moviePosterRedownloadService;
    private final TMDBMovieLoaderService tmdbMovieLoaderService;
    private final TMDBSeriesLoaderService tmdbSeriesLoaderService;
    private final GoogleBooksLoaderService googleBooksLoaderService;
    private final ImageStorageService imageStorageService;
    private final TMDBClient tmdbClient;

    @Transactional
    public RepairResult repairMoviePosters(boolean includeExisting) {
        int repaired = 0;
        int skipped = 0;
        int errors = 0;
        for (Movie movie : movieRepository.findAll()) {
            if (!includeExisting && !shouldReloadImage(movie.getPosterLocalPath())) {
                skipped++;
                continue;
            }
            try {
                if (tmdbMovieLoaderService.repairMovieMetadataAndCast(movie, includeExisting)) {
                    repaired++;
                } else if (moviePosterRedownloadService.redownloadMoviePoster(movie)) {
                    repaired++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Error reparando poster de película id={}: {}", movie.getId(), e.getMessage());
            }
        }
        return new RepairResult(repaired, skipped, errors);
    }

    @Transactional
    public RepairResult repairSeriesPostersAndCast(boolean includeExistingImages) {
        int repaired = 0;
        int skipped = 0;
        int errors = 0;
        for (TvShow series : tvShowRepository.findAll()) {
            try {
                if (series.getTmdbId() == null) {
                    skipped++;
                    continue;
                }
                TvShow updated = tmdbSeriesLoaderService.importOrUpdateByTmdb(series.getTmdbId());
                boolean changed = updated != null && repairSeriesPoster(updated, includeExistingImages);
                if (changed || updated != null) {
                    repaired++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Error reparando serie id={}: {}", series.getId(), e.getMessage());
            }
        }
        return new RepairResult(repaired, skipped, errors);
    }

    @Transactional
    public RepairResult repairBookCoversAndAuthors(boolean includeExistingImages) {
        int repaired = 0;
        int skipped = 0;
        int errors = 0;
        for (Book book : bookRepository.findAll()) {
            try {
                if (book.getGoogleBooksId() == null || book.getGoogleBooksId().isBlank()) {
                    skipped++;
                    continue;
                }
                boolean changed = googleBooksLoaderService.repairBookMetadataAndCover(book, includeExistingImages);
                if (changed) {
                    repaired++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Error reparando libro id={}: {}", book.getId(), e.getMessage());
            }
        }
        return new RepairResult(repaired, skipped, errors);
    }

    @Transactional
    public RepairResult repairPeopleImages(boolean includeExistingImages) {
        int repaired = 0;
        int skipped = 0;
        int errors = 0;

        for (Actor actor : actorRepository.findAll()) {
            try {
                if (repairActorImage(actor, includeExistingImages)) {
                    repaired++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Error reparando imagen de actor id={}: {}", actor.getId(), e.getMessage());
            }
        }

        for (Director director : directorRepository.findAll()) {
            try {
                if (repairDirectorImage(director, includeExistingImages)) {
                    repaired++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Error reparando imagen de director id={}: {}", director.getId(), e.getMessage());
            }
        }

        return new RepairResult(repaired, skipped, errors);
    }

    private boolean repairSeriesPoster(TvShow series, boolean includeExistingImages) {
        if (!includeExistingImages && !shouldReloadImage(series.getPosterLocalPath())) {
            return false;
        }
        if (series.getTmdbId() == null) {
            return false;
        }
        JsonNode details = tmdbClient.getTvDetails(series.getTmdbId());
        String posterPath = details == null ? series.getPosterPath() : details.path("poster_path").asText(series.getPosterPath());
        if (posterPath == null || posterPath.isBlank()) {
            return false;
        }
        series.setPosterPath(posterPath);
        String localPath = imageStorageService.forceDownloadAndStoreImage(
                tmdbClient.buildImageUrl(posterPath),
                AppConstants.POSTERS_SUBFOLDER,
                "series_" + series.getTmdbId());
        if (localPath == null || localPath.isBlank()) {
            return false;
        }
        series.setPosterLocalPath(localPath);
        tvShowRepository.save(series);
        return true;
    }

    private boolean repairActorImage(Actor actor, boolean includeExistingImages) {
        if (!includeExistingImages && !shouldReloadImage(actor.getProfileLocalPath())) {
            return false;
        }
        if (actor.getTmdbId() == null || actor.getProfilePath() == null || actor.getProfilePath().isBlank()) {
            return false;
        }
        String localPath = imageStorageService.forceDownloadAndStoreImage(
                tmdbClient.buildImageUrl(actor.getProfilePath()),
                AppConstants.PROFILES_SUBFOLDER,
                AppConstants.ACTOR_FILE_PREFIX + actor.getTmdbId());
        if (localPath == null || localPath.isBlank()) {
            return false;
        }
        actor.setProfileLocalPath(localPath);
        actorRepository.save(actor);
        return true;
    }

    private boolean repairDirectorImage(Director director, boolean includeExistingImages) {
        if (!includeExistingImages && !shouldReloadImage(director.getProfileLocalPath())) {
            return false;
        }
        if (director.getTmdbId() == null || director.getProfilePath() == null || director.getProfilePath().isBlank()) {
            return false;
        }
        String localPath = imageStorageService.forceDownloadAndStoreImage(
                tmdbClient.buildImageUrl(director.getProfilePath()),
                AppConstants.PROFILES_SUBFOLDER,
                AppConstants.DIRECTOR_FILE_PREFIX + director.getTmdbId());
        if (localPath == null || localPath.isBlank()) {
            return false;
        }
        director.setProfileLocalPath(localPath);
        directorRepository.save(director);
        return true;
    }

    private boolean shouldReloadImage(String localPath) {
        return localPath == null
                || localPath.isBlank()
                || localPath.startsWith("/images/")
                || localPath.startsWith("images/");
    }

    public record RepairResult(int repaired, int skipped, int errors) {}
}
