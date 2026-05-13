package alicanteweb.pelisapp.config;

import alicanteweb.pelisapp.constants.AppConstants;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.service.TMDBSeriesLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

/**
 * Carga inicial de series al arrancar la aplicación.
 * Mismo patrón que ForceMovieLoader pero para series de TV via TMDB.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class ForceSeriesLoader implements CommandLineRunner {

    private final TMDBSeriesLoaderService tmdbSeriesLoaderService;
    private final TvShowRepository tvShowRepository;

    @Value("${app.tmdb.series.load-on-startup:true}")
    private boolean loadOnStartup;

    @Override
    @Async
    public void run(String... args) {
        log.info(AppConstants.LOG_SEPARATOR);
        log.info("{} FORCE SERIES LOADER - Carga inicial de series TMDB", AppConstants.LOG_FIRE_EMOJI);
        log.info(AppConstants.LOG_SEPARATOR);

        if (!loadOnStartup) {
            log.info("{} Carga automática de series desactivada (app.tmdb.series.load-on-startup=false)",
                    AppConstants.LOG_INFO_EMOJI);
            return;
        }

        try {
            long currentCount = tvShowRepository.count();
            log.info("{} Series actuales en BD: {}", AppConstants.LOG_INFO_EMOJI, currentCount);

            if (currentCount < AppConstants.MINIMUM_SERIES_FOR_STARTUP) {
                log.info("{} Iniciando carga de series desde TMDB...", AppConstants.LOG_FIRE_EMOJI);

                int popular = tmdbSeriesLoaderService.importPopularSeries(AppConstants.DEFAULT_SERIES_PAGES_TO_LOAD);
                int topRated = tmdbSeriesLoaderService.importTopRatedSeries(AppConstants.DEFAULT_SERIES_PAGES_TO_LOAD);
                long newCount = tvShowRepository.count();

                log.info(AppConstants.LOG_SEPARATOR);
                log.info("{} CARGA DE SERIES COMPLETADA", AppConstants.LOG_SUCCESS_EMOJI);
                log.info("{} Series antes: {}", AppConstants.LOG_INFO_EMOJI, currentCount);
                log.info("{} Series ahora: {}", AppConstants.LOG_INFO_EMOJI, newCount);
                log.info("{} Populares: {} | Top rated: {}", AppConstants.LOG_INFO_EMOJI, popular, topRated);
                log.info(AppConstants.LOG_SEPARATOR);
            } else {
                log.info("{} Ya hay {} series en la BD, no es necesario cargar",
                        AppConstants.LOG_SUCCESS_EMOJI, currentCount);
            }
        } catch (Exception e) {
            log.error("{} ERROR en ForceSeriesLoader: {}", AppConstants.LOG_ERROR_EMOJI, e.getMessage(), e);
        }
    }
}
