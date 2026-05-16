package alicanteweb.pelisapp.exception;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta estándar para errores de la API.
 */
@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}
