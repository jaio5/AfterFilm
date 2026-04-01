package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Carga libros automáticamente al iniciar la aplicación si la BD está vacía.
 * Equivalente a TMDBMovieLoaderService para películas.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class GoogleBooksStartupLoader {

    private final GoogleBooksLoaderService googleBooksLoaderService;
    private final BookRepository bookRepository;

    @Value("${app.google-books.load-on-startup:false}")
    private boolean loadOnStartup;

    @Value("${app.google-books.startup-max-per-query:20}")
    private int maxPerQuery;

    private static final String[] STARTUP_QUERIES = {
        "novela",
        "clásicos literatura",
        "ciencia ficción",
        "thriller",
        "historia",
        "fantasía"
    };

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        if (!loadOnStartup) {
            log.debug("Carga automática de libros desactivada (app.google-books.load-on-startup=false)");
            return;
        }

        long existingCount = bookRepository.count();
        if (existingCount > 0) {
            log.info("Catálogo de libros ya contiene {} libros, omitiendo importación inicial.", existingCount);
            return;
        }

        log.info("Iniciando importación automática de libros desde Google Books...");
        int total = 0;
        for (String query : STARTUP_QUERIES) {
            try {
                int count = googleBooksLoaderService.searchAndImport(query, maxPerQuery);
                total += count;
                log.info("  → '{}': {} libros importados", query, count);
            } catch (Exception e) {
                log.warn("  → '{}': error al importar — {}", query, e.getMessage());
            }
        }
        log.info("Importación inicial completada: {} libros en total.", total);
    }
}