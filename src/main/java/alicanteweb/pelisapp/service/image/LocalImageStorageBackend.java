package alicanteweb.pelisapp.service.image;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
final class LocalImageStorageBackend implements ImageStorageBackend {

    private final Path storagePath;
    private final String serveBase;

    LocalImageStorageBackend(String storagePath, String serveBase) throws IOException {
        this.storagePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.serveBase = normalizeServeBase(serveBase);
        Files.createDirectories(this.storagePath);

        log.info("📂 Almacenamiento local de imágenes configurado:");
        log.info("  📍 Ruta de almacenamiento: {}", this.storagePath);
        log.info("  🌐 Base URL para servir: {}", this.serveBase);
    }

    @Override
    public String saveImage(InputStream inputStream, String filename, String subfolder) throws IOException {
        validateFilename(filename);

        Path subfolderPath = storagePath.resolve(safeSegment(subfolder));
        Files.createDirectories(subfolderPath);

        String sanitizedFilename = sanitizeFilename(filename);
        Path targetPath = subfolderPath.resolve(sanitizedFilename);

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return resolveStoredPath(filename, subfolder);
    }

    @Override
    public boolean exists(String filename, String subfolder) {
        Path targetPath = storagePath.resolve(safeSegment(subfolder)).resolve(sanitizeFilename(filename));
        return Files.exists(targetPath);
    }

    @Override
    public String resolveStoredPath(String filename, String subfolder) {
        return serveBase + "/" + safeSegment(subfolder) + "/" + sanitizeFilename(filename);
    }

    @Override
    public int deleteDuplicates(String subfolder) {
        int deleted = 0;
        try {
            Path storageDir = storagePath.resolve(safeSegment(subfolder));
            if (!Files.exists(storageDir) || !Files.isDirectory(storageDir)) {
                log.warn("No existe la carpeta de imágenes: {}", storageDir);
                return 0;
            }

            Map<String, Path> hashToFile = new HashMap<>();
            Set<Path> duplicates = new HashSet<>();
            try (java.util.stream.Stream<Path> stream = Files.list(storageDir)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    try (InputStream in = Files.newInputStream(file)) {
                        byte[] content = in.readAllBytes();
                        String hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(content));
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

    private String normalizeServeBase(String serveBase) {
        String base = serveBase == null || serveBase.isBlank() ? "/images" : serveBase.trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Nombre de archivo no puede estar vacío");
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_");
    }

    private String safeSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "default";
        }
        return segment.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}