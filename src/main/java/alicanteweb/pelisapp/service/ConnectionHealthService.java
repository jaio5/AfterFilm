package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.ConnectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Servicio especializado para verificar conexiones de servicios externos.
 * Aplica principio de responsabilidad única (SRP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionHealthService {

    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;

    @Value("${app.tmdb.bearer-token:}")
    private String tmdbBearerToken;

    @Value("${app.moderation.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:}")
    private String emailHost;

    /**
     * Verifica la conexión a la base de datos
     */
    public ConnectionStatus checkDatabaseConnection() {
        Instant start = Instant.now();
        try {
            try (Connection connection = dataSource.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    Duration responseTime = Duration.between(start, Instant.now());
                    log.debug("✅ Base de datos conectada - {}ms", responseTime.toMillis());
                    return createSuccessStatus("Conectada exitosamente", responseTime.toMillis());
                }
            }
            return createErrorStatus("Conexión nula o cerrada", Duration.between(start, Instant.now()).toMillis(), null);
        } catch (SQLException e) {
            Duration responseTime = Duration.between(start, Instant.now());
            log.error("❌ Error en conexión de base de datos: {}", e.getMessage());
            return createErrorStatus("Error de conexión: " + e.getMessage(), responseTime.toMillis(), e.getMessage());
        }
    }

    /**
     * Verifica la conexión con TMDB API
     */
    public ConnectionStatus checkTmdbConnection() {
        Instant start = Instant.now();

        if (tmdbBearerToken == null || tmdbBearerToken.trim().isEmpty()) {
            Duration responseTime = Duration.between(start, Instant.now());
            log.warn("⚠️ Token de TMDB no configurado");
            return createErrorStatus("Token no configurado", responseTime.toMillis(), "BEARER_TOKEN_MISSING");
        }

        try {
            String testUrl = tmdbBaseUrl + "/configuration";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbBearerToken.trim());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);

            Duration responseTime = Duration.between(start, Instant.now());

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("✅ TMDB API conectada - {}ms", responseTime.toMillis());
                return createSuccessStatus("API funcionando correctamente", responseTime.toMillis());
            } else {
                log.warn("⚠️ TMDB API respondió con código: {}", response.getStatusCode());
                return createErrorStatus("Código de respuesta: " + response.getStatusCode(),
                                       responseTime.toMillis(), response.getStatusCode().toString());
            }

        } catch (ResourceAccessException e) {
            Duration responseTime = Duration.between(start, Instant.now());
            log.error("❌ Timeout o error de red en TMDB: {}", e.getMessage());
            return createErrorStatus("Error de conexión: timeout o red", responseTime.toMillis(), "NETWORK_ERROR");
        } catch (Exception e) {
            Duration responseTime = Duration.between(start, Instant.now());
            log.error("❌ Error en TMDB API: {}", e.getMessage());

            String errorType = e.getMessage().contains("401") ? "TOKEN_INVALID" : "API_ERROR";
            return createErrorStatus("Error en API: " + e.getMessage(), responseTime.toMillis(), errorType);
        }
    }

    /**
     * Verifica la conexión con Ollama
     */
    public ConnectionStatus checkOllamaConnection() {
        Instant start = Instant.now();
        try {
            String healthUrl = ollamaUrl + "/api/version";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            Duration responseTime = Duration.between(start, Instant.now());

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("✅ Ollama conectado - {}ms", responseTime.toMillis());
                return createSuccessStatus("Servicio funcionando", responseTime.toMillis());
            } else {
                return createErrorStatus("Respuesta inesperada: " + response.getStatusCode(),
                                       responseTime.toMillis(), response.getStatusCode().toString());
            }

        } catch (ResourceAccessException e) {
            Duration responseTime = Duration.between(start, Instant.now());
            log.error("❌ Ollama no disponible en {}: {}", ollamaUrl, e.getMessage());
            return createErrorStatus("Servicio no disponible", responseTime.toMillis(), "SERVICE_UNAVAILABLE");
        } catch (Exception e) {
            Duration responseTime = Duration.between(start, Instant.now());
            log.error("❌ Error verificando Ollama: {}", e.getMessage());
            return createErrorStatus("Error de conexión: " + e.getMessage(), responseTime.toMillis(), "CONNECTION_ERROR");
        }
    }

    /**
     * Verifica la configuración de email
     */
    public ConnectionStatus checkEmailConfiguration() {
        Instant start = Instant.now();
        Duration responseTime = Duration.between(start, Instant.now());

        if (!emailEnabled) {
            log.debug("📧 Email deshabilitado en configuración");
            return createWarningStatus("Email deshabilitado", responseTime.toMillis());
        }

        if (emailHost == null || emailHost.trim().isEmpty()) {
            log.warn("⚠️ Host de email no configurado");
            return createErrorStatus("Host no configurado", responseTime.toMillis(), "HOST_MISSING");
        }

        log.debug("✅ Configuración de email presente");
        return createSuccessStatus("Configurado correctamente", responseTime.toMillis());
    }

    /**
     * Verifica el estado general del servidor
     */
    public ConnectionStatus checkServerHealth() {
        Instant start = Instant.now();

        // Verificaciones básicas del servidor
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

        Duration responseTime = Duration.between(start, Instant.now());

        if (memoryUsagePercent > 90) {
            log.warn("⚠️ Uso de memoria alto: {}%", String.format("%.1f", memoryUsagePercent));
            return createWarningStatus(String.format("Memoria alta: %.1f%%", memoryUsagePercent), responseTime.toMillis());
        }

        log.debug("✅ Servidor funcionando - Memoria: {}%", String.format("%.1f", memoryUsagePercent));
        return createSuccessStatus(String.format("Funcionando - Memoria: %.1f%%", memoryUsagePercent), responseTime.toMillis());
    }

    // Métodos de utilidad para crear status
    private ConnectionStatus createSuccessStatus(String message, long responseTime) {
        return ConnectionStatus.builder()
                .connected(true)
                .message(message)
                .responseTimeMs(responseTime)
                .lastChecked(Instant.now())
                .build();
    }

    private ConnectionStatus createWarningStatus(String message, long responseTime) {
        return ConnectionStatus.builder()
                .connected(true)
                .message(message)
                .responseTimeMs(responseTime)
                .lastChecked(Instant.now())
                .warning(true)
                .build();
    }

    private ConnectionStatus createErrorStatus(String message, long responseTime, String error) {
        return ConnectionStatus.builder()
                .connected(false)
                .message(message)
                .responseTimeMs(responseTime)
                .lastChecked(Instant.now())
                .error(error)
                .build();
    }
}
