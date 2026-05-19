package alicanteweb.pelisapp.service.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SupabaseImageStorage {

    private final String provider;
    private final String supabaseUrl;
    private final String serviceRole;
    private final String bucket;
    private final String prefix;
    private final String publicBaseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SupabaseImageStorage(@Value("${app.images.storage.provider:local}") String provider,
                                @Value("${app.images.supabase.url:}") String supabaseUrl,
                                @Value("${app.images.supabase.service-role:}") String serviceRole,
                                @Value("${app.images.supabase.bucket:}") String bucket,
                                @Value("${app.images.supabase.prefix:afterfilm/images}") String prefix,
                                @Value("${app.images.supabase.public-base-url:}") String publicBaseUrl) {
        this.provider = clean(provider);
        this.supabaseUrl = trimTrailingSlash(clean(supabaseUrl));
        this.serviceRole = clean(serviceRole);
        this.bucket = clean(bucket);
        this.prefix = trimSlashes(clean(prefix));
        this.publicBaseUrl = trimTrailingSlash(clean(publicBaseUrl));
    }

    public boolean isEnabled() {
        return "supabase".equalsIgnoreCase(provider) && isConfigured();
    }

    public boolean wantsSupabase() {
        return "supabase".equalsIgnoreCase(provider);
    }

    public boolean isConfigured() {
        return !supabaseUrl.isBlank() && !serviceRole.isBlank() && !bucket.isBlank();
    }

    public String upload(byte[] content, String subfolder, String filename, String contentType) throws IOException, InterruptedException {
        if (!isEnabled()) {
            throw new IllegalStateException("Supabase Storage no está configurado correctamente");
        }

        String objectKey = objectKey(subfolder, filename);
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + urlEncode(bucket) + "/" + encodePath(objectKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + serviceRole)
                .header("apikey", serviceRole)
                .header("Content-Type", contentTypeFor(filename, contentType))
                .header("x-upsert", "true")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase Storage respondió HTTP " + response.statusCode() + ": " + response.body());
        }

        String publicUrl = publicUrl(objectKey);
        log.info("Imagen subida a Supabase Storage: {}", publicUrl);
        return publicUrl;
    }

    public boolean isSupabasePublicUrl(String url) {
        if (url == null || url.isBlank() || !isConfigured()) {
            return false;
        }
        String cleanUrl = trimTrailingSlash(url);
        String defaultBase = supabaseUrl + "/storage/v1/object/public/" + urlEncode(bucket);
        String base = publicBaseUrl.isBlank() ? defaultBase : publicBaseUrl;
        return cleanUrl.startsWith(base + "/") || cleanUrl.equals(base);
    }

    public String publicUrlForLegacyPath(String path) {
        if (!wantsSupabase() || !isConfigured() || path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String cleanPath = path.trim().replace("\\", "/");
        if (cleanPath.startsWith("/images/")) {
            cleanPath = cleanPath.substring("/images/".length());
        } else if (cleanPath.startsWith("images/")) {
            cleanPath = cleanPath.substring("images/".length());
        } else if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        return publicUrl(trimSlashes(cleanPath));
    }

    private String publicUrl(String objectKey) {
        String base = publicBaseUrl.isBlank()
                ? supabaseUrl + "/storage/v1/object/public/" + urlEncode(bucket)
                : publicBaseUrl;
        return base + "/" + encodePath(objectKey);
    }

    private String objectKey(String subfolder, String filename) {
        String cleanSubfolder = trimSlashes(clean(subfolder));
        String cleanFilename = sanitizeFilename(filename);
        return Arrays.stream(new String[]{prefix, cleanSubfolder, cleanFilename})
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining("/"));
    }

    private String contentTypeFor(String filename, String fallback) {
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed.jpg";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_");
    }

    private String encodePath(String value) {
        return Arrays.stream(value.split("/"))
                .map(this::urlEncode)
                .collect(Collectors.joining("/"));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String clean = clean(value);
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private String trimSlashes(String value) {
        String clean = clean(value).replace("\\", "/");
        while (clean.startsWith("/")) clean = clean.substring(1);
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }
}
