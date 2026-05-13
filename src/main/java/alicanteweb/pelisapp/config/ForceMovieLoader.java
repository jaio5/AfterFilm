package alicanteweb.pelisapp.config;

import alicanteweb.pelisapp.constants.AppConstants;
import alicanteweb.pelisapp.service.DirectMovieLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Carga inicial de películas al arrancar la aplicación
 *
 * Aplica el patrón Template Method y principios de código limpio:
 * - Una sola responsabilidad: cargar películas al inicio
 * - Nombres descriptivos y constantes centralizadas
 * - Métodos pequeños y enfocados
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class ForceMovieLoader implements CommandLineRunner {

    private final DirectMovieLoader directMovieLoader;

    @Value("${app.tmdb.load-on-startup:false}")
    private boolean loadOnStartup;

    @Override
    public void run(String... args) {
        logStartupBanner();

        try {
            if (!loadOnStartup) {
                log.info("{} Carga automática de películas desactivada (app.tmdb.load-on-startup=false)",
                        AppConstants.LOG_INFO_EMOJI);
                return;
            }
            executeMovieLoadingProcess();
        } catch (Exception e) {
            logError(e);
        }
    }

    private void logStartupBanner() {
        log.info(AppConstants.LOG_SEPARATOR);
        log.info("{} FORCE MOVIE LOADER - VERSIÓN DIRECTA Y SIMPLE", AppConstants.LOG_FIRE_EMOJI);
        log.info(AppConstants.LOG_SEPARATOR);
    }

    private void executeMovieLoadingProcess() {
        long currentCount = directMovieLoader.getMovieCount();
        log.info("{} Películas actuales en BD: {}", AppConstants.LOG_INFO_EMOJI, currentCount);

        if (shouldLoadMovies(currentCount)) {
            loadMovies(currentCount);
        } else {
            logSkippingLoad(currentCount);
        }
    }

    private boolean shouldLoadMovies(long currentCount) {
        return currentCount < AppConstants.MINIMUM_MOVIES_FOR_STARTUP;
    }

    private void loadMovies(long currentCount) {
        logLoadingStart();

        int loaded = directMovieLoader.loadMoviesDirectly(AppConstants.DEFAULT_PAGES_TO_LOAD);
        long newCount = directMovieLoader.getMovieCount();

        logLoadingResults(currentCount, newCount, loaded);
    }

    private void logLoadingStart() {
        log.info("{} Iniciando carga DIRECTA de películas desde TMDB...", AppConstants.LOG_FIRE_EMOJI);
        log.info("⏱️  Esto puede tardar 1-2 minutos...");
    }

    private void logLoadingResults(long currentCount, long newCount, int loaded) {
        log.info(AppConstants.LOG_SEPARATOR);
        log.info("{} CARGA DIRECTA COMPLETADA", AppConstants.LOG_SUCCESS_EMOJI);
        log.info("{} Películas antes: {}", AppConstants.LOG_INFO_EMOJI, currentCount);
        log.info("{} Películas ahora: {}", AppConstants.LOG_INFO_EMOJI, newCount);
        log.info("{} Nuevas cargadas: {}", AppConstants.LOG_INFO_EMOJI, loaded);
        log.info(AppConstants.LOG_SEPARATOR);

        if (loaded > 0) {
            log.info("{} ¡ÉXITO! Se cargaron {} películas DIRECTAMENTE desde TMDB",
                     AppConstants.LOG_FIRE_EMOJI, loaded);
        } else {
            log.warn("{} No se cargaron películas nuevas. Verifica Bearer Token o conexión TMDB",
                     AppConstants.LOG_WARNING_EMOJI);
        }
    }

    private void logSkippingLoad(long currentCount) {
        log.info("{} Ya hay {} películas en la BD, no es necesario cargar",
                 AppConstants.LOG_SUCCESS_EMOJI, currentCount);
    }

    private void logError(Exception e) {
        log.error("{} ERROR en ForceMovieLoader: {}", AppConstants.LOG_ERROR_EMOJI, e.getMessage(), e);
    }
}
