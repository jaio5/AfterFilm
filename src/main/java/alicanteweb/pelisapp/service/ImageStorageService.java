package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.service.image.ImageDownloader;
import alicanteweb.pelisapp.service.image.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Servicio para descargar imágenes desde TMDB y delegar su almacenamiento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final ImageStorage imageStorage;
    private final ImageDownloader imageDownloader;

    /**
     * Descarga una imagen desde una URL y la guarda localmente.
     * @param imageUrl URL completa de la imagen (por ejemplo, <a href="https://image.tmdb.org/t/p/w500/abc.jpg">https://image.tmdb.org/t/p/w500/abc.jpg</a>)
     * @param subfolder subcarpeta donde guardar (ej: "posters", "backdrops")
     * @param filename nombre del archivo sin extensión
     * @return ruta relativa donde se guardó la imagen, o null si falló
     */
    public String downloadAndStoreImage(String imageUrl, String subfolder, String filename) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            String fullFilename = filename + "." + imageDownloader.extractExtension(imageUrl);

            if (imageStorage.exists(fullFilename, subfolder)) {
                String storedPath = imageStorage.resolveStoredPath(fullFilename, subfolder);
                log.debug("Image already exists: {}", storedPath);
                return storedPath;
            }

            try (InputStream imageStream = imageDownloader.downloadImage(imageUrl)) {
                String storedPath = imageStorage.saveImage(imageStream, fullFilename, subfolder);
                log.info("Downloaded image: {} -> {}", imageUrl, storedPath);
                return storedPath;
            }

        } catch (IOException e) {
            log.error("Error downloading image {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Fuerza la redescarga de una imagen, incluso si ya existe localmente.
     * @param imageUrl URL completa de la imagen
     * @param subfolder subcarpeta donde guardar
     * @param filename nombre del archivo sin extensión
     * @return ruta relativa donde se guardó la imagen, o null si falló
     */
    public String forceDownloadAndStoreImage(String imageUrl, String subfolder, String filename) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            String fullFilename = filename + "." + imageDownloader.extractExtension(imageUrl);

            try (InputStream imageStream = imageDownloader.downloadImage(imageUrl)) {
                String storedPath = imageStorage.saveImage(imageStream, fullFilename, subfolder);
                log.info("Force downloaded image: {} -> {}", imageUrl, storedPath);
                return storedPath;
            }

        } catch (IOException e) {
            log.error("Error force downloading image {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Elimina archivos duplicados en la subcarpeta indicada (por hash de contenido).
     * Devuelve el número de archivos eliminados.
     */
    public int deleteDuplicates(String subfolder) {
        return imageStorage.deleteDuplicates(subfolder);
    }
}
