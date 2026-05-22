package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.service.image.SupabaseImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Servicio para almacenar imágenes descargadas desde TMDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    @Value("${app.images.storage-path:./data/images}")
    private String storageBasePath;

    @Value("${app.images.serve-base:/images}")
    private String serveBasePath;

    private final SupabaseImageStorage supabaseImageStorage;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String downloadAndStoreImage(String imageUrl, String subfolder, String filename) {
        return downloadAndStoreImage(imageUrl, subfolder, filename, false);
    }

    public String forceDownloadAndStoreImage(String imageUrl, String subfolder, String filename) {
        return downloadAndStoreImage(imageUrl, subfolder, filename, true);
    }

    private String downloadAndStoreImage(String imageUrl, String subfolder, String filename, boolean force) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            String extension = getImageExtension(imageUrl);
            String fullFilename = filename + extension;

            Path storageDir = Paths.get(storageBasePath, subfolder);
            Path targetPath = storageDir.resolve(fullFilename);

            if (!force && Files.exists(targetPath)) {
                String supabaseUrl = uploadExistingLocalImage(targetPath, subfolder, fullFilename, extension);
                if (supabaseUrl != null) {
                    return supabaseUrl;
                }
                String relativePath = serveBasePath + "/" + subfolder + "/" + fullFilename;
                log.debug("Image already exists: {}", relativePath);
                return relativePath;
            }

            byte[] imageBytes = downloadBytes(imageUrl);

            if (supabaseImageStorage.isEnabled()) {
                return supabaseImageStorage.upload(imageBytes, subfolder, fullFilename, contentType(extension));
            }

            if (supabaseImageStorage.wantsSupabase()) {
                log.warn("IMAGES_STORAGE_PROVIDER=supabase pero faltan variables de Supabase; se usa almacenamiento local como fallback");
            }

            Files.createDirectories(storageDir);
            Files.copy(new ByteArrayInputStream(imageBytes), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = serveBasePath + "/" + subfolder + "/" + fullFilename;
            log.info("Downloaded image: {} -> {}", imageUrl, relativePath);
            return relativePath;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error downloading image {}", imageUrl, e);
            return null;
        }
    }

    private byte[] downloadBytes(String imageUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .header("User-Agent", "AfterFilm/1.0")
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("No se pudo descargar " + imageUrl + " HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String getImageExtension(String url) {
        if (url.contains(".jpg") || url.contains(".jpeg")) return ".jpg";
        if (url.contains(".png")) return ".png";
        if (url.contains(".webp")) return ".webp";
        return ".jpg";
    }

    private String contentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private String uploadExistingLocalImage(Path targetPath, String subfolder, String filename, String extension) {
        if (!supabaseImageStorage.isEnabled()) {
            return null;
        }

        try {
            return supabaseImageStorage.upload(
                    Files.readAllBytes(targetPath), subfolder, filename, contentType(extension));
        } catch (IOException e) {
            log.warn("No se pudo subir imagen local existente {} a Supabase: {}", targetPath, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Subida a Supabase interrumpida para {}", targetPath);
            return null;
        }
    }

    public int deleteDuplicates(String subfolder) {
        int deleted = 0;
        try {
            Path storageDir = Paths.get(storageBasePath, subfolder);
            if (!Files.exists(storageDir) || !Files.isDirectory(storageDir)) {
                log.warn("No existe la carpeta de imágenes: {}", storageDir);
                return 0;
            }
            java.util.Map<String, Path> hashToFile = new java.util.HashMap<>();
            java.util.Set<Path> duplicates = new java.util.HashSet<>();
            try (java.util.stream.Stream<Path> stream = Files.list(storageDir)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    try (InputStream in = Files.newInputStream(file)) {
                        byte[] content = in.readAllBytes();
                        String hash = java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(content));
                        if (hashToFile.containsKey(hash)) {
                            duplicates.add(file);
                        } else {
                            hashToFile.put(hash, file);
                        }
                    } catch (Exception e) {
                        log.warn("Error leyendo archivo {}: {}", file, e.getMessage());
                    }
                }
            }
            for (Path dup : duplicates) {
                try {
                    Files.delete(dup);
                    deleted++;
                    log.info("Archivo duplicado eliminado: {}", dup);
                } catch (Exception e) {
                    log.warn("No se pudo eliminar {}: {}", dup, e.getMessage());
                }
            }
            log.info("Eliminados {} archivos duplicados en {}", deleted, storageDir);
        } catch (Exception e) {
            log.error("Error eliminando duplicados en {}: {}", subfolder, e.getMessage());
        }
        return deleted;
    }
}
