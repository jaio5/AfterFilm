package alicanteweb.pelisapp.service.image;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Slf4j
final class S3ImageStorageBackend implements ImageStorageBackend {

    private final String bucket;
    private final String region;
    private final String prefix;
    private final String publicBaseUrl;
    private final S3Client s3Client;

    S3ImageStorageBackend(String bucket, String region, String prefix, String publicBaseUrl) {
        this.bucket = bucket;
        this.region = region == null || region.isBlank() ? "us-east-1" : region;
        this.prefix = normalizePrefix(prefix);
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.s3Client = S3Client.builder().region(Region.of(this.region)).build();

        log.info("☁️ Almacenamiento S3 configurado:");
        log.info("  🪣 Bucket: {}", this.bucket);
        log.info("  🌍 Región: {}", this.region);
        log.info("  📁 Prefijo: {}", this.prefix);
    }

    @Override
    public String saveImage(InputStream inputStream, String filename, String subfolder) throws IOException {
        validateBucket();

        String sanitizedFilename = sanitizeFilename(filename);
        String key = buildKey(subfolder, sanitizedFilename);

        try {
            byte[] bytes = inputStream.readAllBytes();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(guessContentType(sanitizedFilename))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            return resolveStoredPath(filename, subfolder);
        } catch (Exception e) {
            throw new IOException("Error subiendo imagen a S3: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String filename, String subfolder) {
        validateBucket();

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(buildKey(subfolder, sanitizeFilename(filename)))
                    .build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    @Override
    public String resolveStoredPath(String filename, String subfolder) {
        String key = buildKey(subfolder, sanitizeFilename(filename));
        if (!publicBaseUrl.isBlank()) {
            return normalizeBaseUrl(publicBaseUrl) + "/" + key;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    @Override
    public int deleteDuplicates(String subfolder) {
        log.info("S3 no ejecuta limpieza de duplicados localmente; operación omitida para {}", subfolder);
        return 0;
    }

    @Override
    public void close() {
        s3Client.close();
    }

    private void validateBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.images.s3.bucket debe configurarse cuando provider=s3");
        }
    }

    private String buildKey(String subfolder, String filename) {
        StringBuilder key = new StringBuilder();
        if (!prefix.isBlank()) {
            key.append(prefix).append('/');
        }
        key.append(safeSegment(subfolder)).append('/').append(filename);
        return key.toString();
    }

    private String normalizePrefix(String rawPrefix) {
        if (rawPrefix == null || rawPrefix.isBlank()) {
            return "pelisapp/images";
        }
        String normalized = rawPrefix.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed.jpg";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String safeSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "default";
        }
        return segment.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}