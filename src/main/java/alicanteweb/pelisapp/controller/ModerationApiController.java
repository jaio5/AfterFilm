package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@Slf4j
public class ModerationApiController {
    private final ModerationService moderationService;

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testModeration(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "approved", false,
                    "message", "Texto vacío"
                ));
            }

            log.info("🧪 TEST de moderación para texto: '{}'",
                    text.length() > 100 ? text.substring(0, 100) + "..." : text);

            ModerationService.ModerationResult result = moderationService.moderateContentSync(text);

            Map<String, Object> response = new HashMap<>();
            response.put("approved", true);
            response.put("score", Math.round(result.toxicityScore() * 100.0) / 100.0);
            response.put("reason", result.reason());
            response.put("ollama_used", result.ollamaUsed());

            log.info("✅ TEST APROBADO - Puntuación: {}, Ollama: {}",
                    String.format("%.2f", result.toxicityScore()), result.ollamaUsed());

            return ResponseEntity.ok(response);

        } catch (ModerationService.ContentModerationException e) {
            log.info("❌ TEST RECHAZADO - Razón: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "approved", false,
                "message", e.getMessage(),
                "reason", "Contenido inapropiado detectado"
            ));
        } catch (Exception e) {
            log.error("⚠️ ERROR en test de moderación: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "approved", false,
                "message", "Error interno: " + e.getMessage()
            ));
        }
    }
}
