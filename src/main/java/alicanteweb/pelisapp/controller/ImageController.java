package alicanteweb.pelisapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Controller
public class ImageController {

    @Value("${app.images.storage.provider:local}")
    private String imageStorageProvider;

    private final Path storagePath;

    public ImageController(@Value("${app.images.storage-path:./data/images}") String storagePath) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String fileName) throws MalformedURLException {
        if ("s3".equalsIgnoreCase(imageStorageProvider)) {
            return ResponseEntity.notFound().build();
        }

        Path file = storagePath.resolve(fileName).normalize();
        if (!file.startsWith(storagePath) || !file.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }
        UrlResource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"").body(resource);
    }
}
