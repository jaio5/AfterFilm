package alicanteweb.pelisapp.service.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;

/**
 * Facade para el almacenamiento de imágenes.
 * Permite usar almacenamiento local o S3 con la misma API.
 */
@Component
@Slf4j
public class ImageStorage {

    private final ImageStorageBackend backend;

    public ImageStorage(
            @Value("${app.images.storage.provider:local}") String storageProvider,
            @Value("${app.images.storage-path:./data/images}") String storagePath,
            @Value("${app.images.serve-base:/images}") String serveBase,
            @Value("${app.images.s3.bucket:}") String s3Bucket,
            @Value("${app.images.s3.region:us-east-1}") String s3Region,
            @Value("${app.images.s3.prefix:pelisapp/images}") String s3Prefix,
            @Value("${app.images.s3.public-base-url:}") String s3PublicBaseUrl
    ) throws IOException {
        if ("s3".equalsIgnoreCase(storageProvider)) {
            this.backend = new S3ImageStorageBackend(s3Bucket, s3Region, s3Prefix, s3PublicBaseUrl);
            log.info("🌐 ImageStorage usando backend S3");
        } else if ("supabase".equalsIgnoreCase(storageProvider)) {
            // Supabase storage configuration is read from env: app.images.supabase.*
            String supabaseUrl = System.getProperty("app.images.supabase.url", System.getenv("SUPABASE_URL"));
            String supabaseServiceRole = System.getProperty("app.images.supabase.service-role", System.getenv("SUPABASE_SERVICE_ROLE"));
            String supabaseBucket = System.getProperty("app.images.supabase.bucket", System.getenv("SUPABASE_BUCKET"));
            String supabasePrefix = System.getProperty("app.images.supabase.prefix", System.getenv("SUPABASE_PREFIX"));
            String supabasePublicBase = System.getProperty("app.images.supabase.public-base-url", System.getenv("SUPABASE_PUBLIC_BASE_URL"));
            this.backend = new SupabaseImageStorageBackend(supabaseUrl, supabaseServiceRole, supabaseBucket, supabasePrefix, supabasePublicBase);
            log.info("☁️ ImageStorage usando backend Supabase Storage");
        } else {
            this.backend = new LocalImageStorageBackend(storagePath, serveBase);
            log.info("📁 ImageStorage usando backend local");
        }
    }

    public String saveImage(InputStream inputStream, String filename, String subfolder) throws IOException {
        return backend.saveImage(inputStream, filename, subfolder);
    }

    public boolean exists(String filename, String subfolder) {
        return backend.exists(filename, subfolder);
    }

    public String resolveStoredPath(String filename, String subfolder) {
        return backend.resolveStoredPath(filename, subfolder);
    }

    public int deleteDuplicates(String subfolder) {
        return backend.deleteDuplicates(subfolder);
    }

    @PreDestroy
    public void shutdown() throws Exception {
        backend.close();
    }
}
