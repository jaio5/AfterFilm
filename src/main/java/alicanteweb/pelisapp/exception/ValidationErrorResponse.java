package alicanteweb.pelisapp.exception;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Respuesta específica para errores de validación.
 */
@Data
@Builder
public class ValidationErrorResponse {
    private int status;
    private String message;
    private Map<String, String> errors;
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}
