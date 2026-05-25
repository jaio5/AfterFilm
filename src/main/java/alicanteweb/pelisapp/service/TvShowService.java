package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.TvShowDetailDTO;
import alicanteweb.pelisapp.dto.TvShowListDTO;
import alicanteweb.pelisapp.dto.CastDTO;
import alicanteweb.pelisapp.dto.CrewDTO;
import alicanteweb.pelisapp.entity.Actor;
import alicanteweb.pelisapp.entity.Director;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.repository.TvShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TvShowService {

    private final TvShowRepository tvShowRepository;
    private final ImageUrlService imageUrlService;

    public Page<TvShowListDTO> getAllSeries(Pageable pageable) {
        Page<TvShow> page = tvShowRepository.findAll(pageable);
        List<TvShowListDTO> dtos = mapWithRatingStats(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public Optional<TvShowDetailDTO> getSeriesById(Long id) {
        return tvShowRepository.findByIdWithCastAndDirectors(id).map(this::toDetailDTO);
    }

    public List<TvShowListDTO> searchSeries(String query) {
        return mapWithRatingStats(tvShowRepository.findByTitleContainingIgnoreCase(query));
    }

    public Page<TvShowListDTO> getSeriesByGenre(String genre, Pageable pageable) {
        Page<TvShow> page = tvShowRepository.findByGenresContainingIgnoreCase(genre, pageable);
        List<TvShowListDTO> dtos = mapWithRatingStats(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public List<String> getAvailableGenres() {
        return tvShowRepository.findAllGenreStrings().stream()
                .flatMap(g -> Arrays.stream(g.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public Optional<TvShowListDTO> getTopRatedThisMonth() {
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        List<TvShow> top = tvShowRepository.findTopRatedThisMonth(startOfMonth, PageRequest.of(0, 1));
        if (!top.isEmpty()) return Optional.of(mapWithRatingStats(top).get(0));
        Page<TvShow> fallback = tvShowRepository.findAll(PageRequest.of(0, 1));
        return fallback.getContent().isEmpty()
                ? Optional.empty()
                : Optional.of(mapWithRatingStats(fallback.getContent()).get(0));
    }

    private List<TvShowListDTO> mapWithRatingStats(List<TvShow> shows) {
        if (shows.isEmpty()) return List.of();
        List<Long> ids = shows.stream().map(TvShow::getId).toList();
        Map<Long, double[]> ratingMap = RatingStatsHelper.buildRatingMap(tvShowRepository.findRatingStatsByIds(ids));
        return shows.stream().map(s -> toListDTO(s, ratingMap.get(s.getId()))).toList();
    }

    private TvShowListDTO toListDTO(TvShow show, double[] stats) {
        TvShowListDTO dto = new TvShowListDTO();
        dto.setId(show.getId());
        dto.setTmdbId(show.getTmdbId());
        dto.setTitle(show.getTitle());
        dto.setOverview(show.getOverview());
        String posterUrl = imageUrlService.seriesPosterUrl(show, "w500");
        dto.setPosterPath(posterUrl);
        dto.setPosterLocalPath(posterUrl);
        dto.setFirstAirDate(show.getFirstAirDate());
        dto.setNumberOfSeasons(show.getNumberOfSeasons());
        dto.setGenres(show.getGenres());
        dto.setStatus(show.getStatus());
        if (stats != null && stats[1] > 0) {
            dto.setReviewCount((int) stats[1]);
            dto.setAvgRating(stats[0]);
        } else {
            dto.setReviewCount(0);
        }
        return dto;
    }

    private TvShowDetailDTO toDetailDTO(TvShow show) {
        TvShowDetailDTO dto = new TvShowDetailDTO();
        dto.setId(show.getId());
        dto.setTmdbId(show.getTmdbId());
        dto.setTitle(show.getTitle());
        dto.setOriginalTitle(show.getOriginalTitle());
        dto.setOverview(show.getOverview());
        String posterUrl = imageUrlService.seriesPosterUrl(show, "w500");
        dto.setPosterPath(posterUrl);
        dto.setPosterLocalPath(posterUrl);
        dto.setBackdropPath(show.getBackdropPath());
        dto.setFirstAirDate(show.getFirstAirDate());
        dto.setNumberOfSeasons(show.getNumberOfSeasons());
        dto.setNumberOfEpisodes(show.getNumberOfEpisodes());
        dto.setGenres(show.getGenres());
        dto.setStatus(show.getStatus());
        dto.setLanguage(show.getLanguage());
        dto.setCastMembers(show.getActors().stream()
                .map(this::toCastDTO)
                .toList());
        dto.setDirectors(show.getDirectors().stream()
                .map(this::toCrewDTO)
                .toList());
        return dto;
    }

    private CastDTO toCastDTO(Actor actor) {
        CastDTO dto = new CastDTO();
        dto.setTmdbId(actor.getTmdbId());
        dto.setName(actor.getName());
        dto.setCharacter("Reparto");
        dto.setProfilePath(actor.getProfilePath());
        dto.setProfileLocalPath(actor.getProfileLocalPath());
        dto.setProfileUrl(imageUrlService.localImageUrlIfAvailable(actor.getProfileLocalPath()));
        if (dto.getProfileUrl() == null) {
            dto.setProfileUrl(imageUrlService.tmdbImageUrl(actor.getProfilePath(), "w185"));
        }
        return dto;
    }

    private CrewDTO toCrewDTO(Director director) {
        CrewDTO dto = new CrewDTO();
        dto.setTmdbId(director.getTmdbId());
        dto.setName(director.getName());
        dto.setJob("Direccion");
        dto.setDepartment("Directing");
        dto.setProfilePath(director.getProfilePath());
        dto.setProfileLocalPath(director.getProfileLocalPath());
        dto.setProfileUrl(imageUrlService.localImageUrlIfAvailable(director.getProfileLocalPath()));
        if (dto.getProfileUrl() == null) {
            dto.setProfileUrl(imageUrlService.tmdbImageUrl(director.getProfilePath(), "w185"));
        }
        return dto;
    }
}
