package alicanteweb.pelisapp.service.image;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Slf4j
final class SupabaseImageStorageBackend implements ImageStorageBackend {

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final String prefix;
    private final String publicBaseUrl;
    private final HttpClient httpClient;

    SupabaseImageStorageBackend(String supabaseUrl, String serviceRoleKey, String bucket, String prefix, String publicBaseUrl) {
        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.trim();
        this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey.trim();
        this.bucket = bucket == null ? "" : bucket.trim();
        this.prefix = normalizePrefix(prefix);
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        log.info("☁️ Supabase Storage backend configurado:");
        log.info("  🔗 Supabase URL: {}", this.supabaseUrl);
        log.info("  🪣 Bucket: {}", this.bucket);
        log.info("  📁 Prefijo: {}", this.prefix);
    }

    @Override
    public String saveImage(InputStream inputStream, String filename, String subfolder) throws IOException {
        validateConfig();

        String sanitized = sanitizeFilename(filename);
        String key = buildKey(subfolder, sanitized);

        try {
            byte[] bytes = readAllBytes(inputStream);

            URI uri = URI.create(String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, key));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", guessContentType(sanitized))
                    .header("x-upsert", "true")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return resolveStoredPath(filename, subfolder);
            }
            throw new IOException("Supabase upload failed: " + response.statusCode() + " " + response.body());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted uploading to Supabase", ie);
        }
    }

    @Override
    public boolean exists(String filename, String subfolder) {
        try {
            String key = buildKey(subfolder, sanitizeFilename(filename));
            URI uri = URI.create(String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, key));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<Void> resp = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String resolveStoredPath(String filename, String subfolder) {
        String key = buildKey(subfolder, sanitizeFilename(filename));
        if (!publicBaseUrl.isBlank()) {
            return normalizeBaseUrl(publicBaseUrl) + "/" + key;
        }
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucket, key);
    }

    @Override
    public int deleteDuplicates(String subfolder) {
        log.info("Supabase backend no elimina duplicados localmente; operación omitida para {}", subfolder);
        return 0;
    }

    private void validateConfig() {
        if (supabaseUrl.isBlank() || serviceRoleKey.isBlank() || bucket.isBlank()) {
            throw new IllegalStateException("Supabase storage requires SUPABASE_URL, SUPABASE_SERVICE_ROLE and SUPABASE_BUCKET");
        }
    }

    private String buildKey(String subfolder, String filename) {
        StringBuilder sb = new StringBuilder();
        if (!prefix.isBlank()) {
            sb.append(prefix).append('/');
        }
        sb.append(safeSegment(subfolder)).append('/').append(filename);
        return sb.toString();
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
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }
}
