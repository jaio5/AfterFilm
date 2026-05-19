package alicanteweb.pelisapp.service.image;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Componente especializado para almacenamiento de imágenes.
 */
@Component
@Slf4j
@Getter
public class ImageStorage {

    private final Path storagePath;
    private final String serveBase;
    private final SupabaseImageStorage supabaseImageStorage;

    public ImageStorage(@Value("${app.images.storage-path:./data/images}") String storagePath,
                        @Value("${app.images.serve-base:/images}") String serveBase,
                        SupabaseImageStorage supabaseImageStorage) throws IOException {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.serveBase = serveBase.endsWith("/") ? serveBase.substring(0, serveBase.length()-1) : serveBase;
        this.supabaseImageStorage = supabaseImageStorage;

        if (!supabaseImageStorage.isEnabled()) {
            Files.createDirectories(this.storagePath);
        }

        log.info("ImageStorage configurado:");
        log.info("  Ruta de almacenamiento local: {}", this.storagePath);
        log.info("  Base URL local para servir: {}", this.serveBase);
        log.info("  Supabase Storage activo: {}", supabaseImageStorage.isEnabled());
    }

    public String saveImage(InputStream inputStream, String filename, String subfolder) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Nombre de archivo no puede estar vacío");
        }

        String sanitizedFilename = sanitizeFilename(filename);

        if (supabaseImageStorage.isEnabled()) {
            try {
                return supabaseImageStorage.upload(inputStream.readAllBytes(), subfolder, sanitizedFilename, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Error subiendo imagen a Supabase", e);
            }
        }

        if (supabaseImageStorage.wantsSupabase()) {
            log.warn("IMAGES_STORAGE_PROVIDER=supabase pero faltan variables de Supabase; se usa almacenamiento local como fallback");
        }

        Path subfolderPath = storagePath.resolve(subfolder);
        Files.createDirectories(subfolderPath);
        Path sanitizedPath = subfolderPath.resolve(sanitizedFilename);

        log.debug("Guardando imagen local: {}/{}", subfolder, sanitizedFilename);
        try {
            Files.copy(inputStream, sanitizedPath, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = subfolder + "/" + sanitizedFilename;
            log.debug("Imagen guardada: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Error guardando imagen {}/{}: {}", subfolder, sanitizedFilename, e.getMessage());
            throw new IOException("Error guardando imagen: " + e.getMessage(), e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_");
    }
}
