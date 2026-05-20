package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.service.image.ImageDownloader;
import alicanteweb.pelisapp.service.image.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Servicio principal para manejo de imágenes.
 * Refactorizado usando ImageDownloader e ImageStorage para cumplir SRP.
 * Actúa como fachada coordinando descarga y almacenamiento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageDownloader imageDownloader;
    private final ImageStorage imageStorage;

    /**
     * Descarga una imagen desde una URL remota y la guarda localmente.
     *
     * @param imageUrl URL de la imagen remota
     * @param prefix prefijo para el nombre del archivo (ej: "movie", "profile")
     * @param subfolder subcarpeta donde guardar (ej: "posters", "profiles")
     * @return ruta relativa del archivo guardado localmente, o null si falla
     */
    public String downloadAndSave(String imageUrl, String prefix, String subfolder) {
        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn("⚠️ URL de imagen vacía, no se puede descargar");
            return null;
        }

        try {
            log.info("🔄 Descargando y guardando imagen: {} -> {}/{}", imageUrl, subfolder, prefix);

            // Generar nombre único para el archivo
            String filename = imageDownloader.generateUniqueFilename(prefix, imageUrl);

            // Descargar imagen
            try (InputStream imageStream = imageDownloader.downloadImage(imageUrl)) {
                String relativePath = imageStorage.saveImage(imageStream, filename, subfolder);
                log.info("✅ Imagen descargada y guardada: {} -> {}", imageUrl, relativePath);
                return relativePath;
            }

        } catch (IOException e) {
            log.error("❌ Error descargando y guardando imagen desde {}: {}", imageUrl, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Error inesperado procesando imagen desde {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }
}
