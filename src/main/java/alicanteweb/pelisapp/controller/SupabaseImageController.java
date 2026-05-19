package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.service.image.SupabaseImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SupabaseImageController {

    private final SupabaseImageStorage supabaseImageStorage;

    @GetMapping("/supabase-images/**")
    public ResponseEntity<byte[]> serveSupabaseImage(HttpServletRequest request) {
        String path = Optional.ofNullable((String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
                .orElse("");
        String objectKey = supabaseImageStorage.objectKeyFromRequestPath(path.replaceFirst("^/supabase-images/?", ""));
        if (objectKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Optional<SupabaseImageStorage.StoredImage> image = supabaseImageStorage.fetch(objectKey);
            if (image.isEmpty()) {
                log.warn("Imagen no encontrada en Supabase Storage: {}", objectKey);
                return ResponseEntity.notFound().build();
            }
            SupabaseImageStorage.StoredImage storedImage = image.get();
            MediaType mediaType = MediaType.parseMediaType(storedImage.contentType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(storedImage.content());
        } catch (Exception e) {
            log.error("No se pudo servir imagen desde Supabase {}: {}", objectKey, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
