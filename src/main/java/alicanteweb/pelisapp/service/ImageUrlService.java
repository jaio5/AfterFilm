package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.service.image.SupabaseImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageUrlService {

    private final Path storagePath;
    private final String serveBase;
    private final SupabaseImageStorage supabaseImageStorage;

    public ImageUrlService(@Value("${app.images.storage-path:./data/images}") String storagePath,
                           @Value("${app.images.serve-base:/images}") String serveBase,
                           SupabaseImageStorage supabaseImageStorage) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.serveBase = normalizeServeBase(serveBase);
        this.supabaseImageStorage = supabaseImageStorage;
    }

    public String moviePosterUrl(Movie movie, String tmdbSize) {
        if (movie == null) {
            return null;
        }
        return posterUrl(movie.getPosterLocalPath(), movie.getPosterPath(), tmdbSize);
    }

    public String seriesPosterUrl(TvShow series, String tmdbSize) {
        if (series == null) {
            return null;
        }
        return posterUrl(series.getPosterLocalPath(), series.getPosterPath(), tmdbSize);
    }

    public String posterUrl(String localPath, String remotePath, String tmdbSize) {
        String localUrl = localImageUrlIfAvailable(localPath);
        if (localUrl != null) {
            return localUrl;
        }
        if (supabaseImageStorage.wantsSupabase()) {
            return null;
        }
        return tmdbImageUrl(remotePath, tmdbSize);
    }

    public String tmdbImageUrl(String path, String tmdbSize) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (supabaseImageStorage.wantsSupabase()) {
            return supabaseImageStorage.isSupabasePublicUrl(path) ? path : null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String cleanSize = tmdbSize == null || tmdbSize.isBlank() ? "w500" : tmdbSize;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return "https://image.tmdb.org/t/p/" + cleanSize + cleanPath;
    }

    public String localImageUrlIfAvailable(String localPath) {
        if (localPath == null || localPath.isBlank()) {
            return null;
        }
        if (localPath.startsWith("http://") || localPath.startsWith("https://")) {
            return localPath;
        }

        String supabaseUrl = supabaseImageStorage.publicUrlForLegacyPath(localPath);
        if (supabaseUrl != null) {
            return supabaseUrl;
        }

        String relativePath = stripServeBase(localPath);
        Path file = storagePath.resolve(relativePath).normalize();
        if (!file.startsWith(storagePath) || !Files.exists(file)) {
            return null;
        }
        return serveBase + "/" + relativePath.replace("\\", "/");
    }

    private String stripServeBase(String path) {
        String clean = path.trim().replace("\\", "/");
        if (clean.startsWith(serveBase + "/")) {
            clean = clean.substring(serveBase.length() + 1);
        } else if (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        return clean;
    }

    private String normalizeServeBase(String value) {
        if (value == null || value.isBlank()) {
            return "/images";
        }
        String clean = value.trim();
        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }
        return clean.endsWith("/") ? clean.substring(0, clean.length() - 1) : clean;
    }
}
