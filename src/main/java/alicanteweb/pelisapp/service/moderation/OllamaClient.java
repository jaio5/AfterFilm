package alicanteweb.pelisapp.service.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente para interactuar con Ollama AI para análisis de contenido tóxico.
 * Aplica principio de responsabilidad única (SRP).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.moderation.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${app.moderation.ollama.model:llama3}")
    private String ollamaModel;

    /**
     * Analiza texto con Ollama usando un prompt especializado para detección de contenido tóxico.
     */
    public OllamaAnalysisResult analyzeContent(String text) {
        try {
            String prompt = buildModerationPrompt(text);
            HttpEntity<Map<String, Object>> request = createOllamaRequest(prompt);

            log.debug("📤 Enviando solicitud a Ollama: {}/api/generate", ollamaUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                ollamaUrl + "/api/generate", request, String.class);

            return handleOllamaResponse(response);

        } catch (Exception e) {
            log.error("Error conectando con Ollama: {}", e.getMessage());
            throw new OllamaException("Error en análisis con Ollama", e);
        }
    }

    /**
     * Construye el prompt de moderación para Ollama.
     */
    private String buildModerationPrompt(String text) {
        return String.format(
            """
            Eres un moderador de contenido experto. Analiza el siguiente texto y determina si contiene:
            - Lenguaje tóxico, ofensivo, abusivo o de odio
            - Insultos, amenazas o acoso hacia otras personas
            - Contenido inapropiado para un sitio de películas

            Texto a analizar: "%s"

            Responde EXACTAMENTE en este formato JSON:
            {
                "toxicity_score": [número entre 0.0 y 1.0],
                "is_toxic": [true/false],
                "harassment_intent": [true si el mensaje tiene intención clara de acosar o insultar a otro usuario, false en caso contrario],
                "reason": "[explicación breve]"
            }

            Ejemplos:
            - Texto limpio: {"toxicity_score": 0.1, "is_toxic": false, "harassment_intent": false, "reason": "Contenido apropiado"}
            - Texto tóxico: {"toxicity_score": 0.9, "is_toxic": true, "harassment_intent": true, "reason": "Contiene insultos dirigidos a un usuario"}
            """, text);
    }

    /**
     * Crea la petición HTTP para Ollama.
     */
    private HttpEntity<Map<String, Object>> createOllamaRequest(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of(
            "temperature", 0.1,
            "top_p", 0.9,
            "max_tokens", 200
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(requestBody, headers);
    }

    /**
     * Maneja la respuesta de Ollama y extrae el resultado.
     */
    private OllamaAnalysisResult handleOllamaResponse(ResponseEntity<String> response) {
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return parseOllamaResponse(response.getBody());
        } else {
            throw new OllamaException("Respuesta inválida de Ollama: " + response.getStatusCode());
        }
    }

    /**
     * Parsea la respuesta JSON de Ollama.
     */
    private OllamaAnalysisResult parseOllamaResponse(String responseBody) {
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            String content = responseJson.path("response").asText();

            log.debug("📥 Respuesta de Ollama: {}", content);

            // Extraer JSON de la respuesta
            String jsonStart = content.contains("{") ? content.substring(content.indexOf("{")) : content;
            String jsonEnd = jsonStart.lastIndexOf("}") >= 0 ? jsonStart.substring(0, jsonStart.lastIndexOf("}") + 1) : jsonStart;

            JsonNode analysisJson = objectMapper.readTree(jsonEnd);

            double toxicityScore = analysisJson.path("toxicity_score").asDouble(0.0);
            boolean isToxic = analysisJson.path("is_toxic").asBoolean(false);
            boolean harassmentIntent = analysisJson.path("harassment_intent").asBoolean(false);
            String reason = analysisJson.path("reason").asText("Sin razón especificada");

            log.debug("✅ Análisis completado: puntuación={}, tóxico={}, acoso={}, razón='{}'",
                     toxicityScore, isToxic, harassmentIntent, reason);

            return new OllamaAnalysisResult(toxicityScore, reason, harassmentIntent);

        } catch (Exception e) {
            log.error("❌ Error parseando respuesta de Ollama: {}", e.getMessage());
            throw new OllamaException("Error procesando respuesta de Ollama", e);
        }
    }

    /**
     * Resultado del análisis de Ollama.
     */
    public record OllamaAnalysisResult(double toxicityScore, String reason, boolean harassmentIntent) {}

    /**
     * Excepción específica para errores de Ollama.
     */
    public static class OllamaException extends RuntimeException {
        public OllamaException(String message) {
            super(message);
        }

        public OllamaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
