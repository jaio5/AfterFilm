package alicanteweb.pelisapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración para servir archivos estáticos de imágenes y videos
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.images.storage-path:./data/images}")
    private String imagesStoragePath;

    @Value("${app.images.serve-base:/images}")
    private String imagesServeBase;

    @Value("${app.movies.storage-path:./data/movies}")
    private String moviesStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configurar para servir imágenes desde el directorio local
        String imageResourceLocation = "file:" + imagesStoragePath + "/";
        String imagePathPattern = imagesServeBase + "/**";

        registry.addResourceHandler(imagePathPattern)
                .addResourceLocations(imageResourceLocation)
                .setCachePeriod(3600); // Cache por 1 hora

        // Configurar para servir archivos de video (pero solo para acceso directo simple)
        // Los endpoints de descarga/streaming con range están en MovieFileController
        String movieResourceLocation = "file:" + moviesStoragePath + "/";
        String moviePathPattern = "/video/**";

        registry.addResourceHandler(moviePathPattern)
                .addResourceLocations(movieResourceLocation)
                .setCachePeriod(0); // Sin cache para videos

        log.info("Configurado servido de imágenes: {} -> {}", imagePathPattern, imageResourceLocation);
        log.info("Configurado servido de videos: {} -> {}", moviePathPattern, movieResourceLocation);
    }
}
