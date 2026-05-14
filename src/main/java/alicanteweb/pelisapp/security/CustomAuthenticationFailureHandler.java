package alicanteweb.pelisapp.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handler personalizado para login fallido
 * Implementa protección contra ataques de fuerza bruta
 */
@Component
@Slf4j
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String remoteAddr = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // Log de seguridad
        log.warn("Login fallido: usuario={}, ip={}, userAgent={}, razon={}",
                username, remoteAddr, userAgent, exception.getClass().getSimpleName());
        log.debug("Detalle del fallo de autenticacion: {}", exception.getMessage());

        HttpSession session = request.getSession();

        // Manejar diferentes tipos de errores de autenticación
        String errorMessage = determineErrorMessage(exception);
        log.debug("Mensaje mostrado al usuario: {}", errorMessage);

        // Implementar protección contra ataques de fuerza bruta
        if (!(exception instanceof DisabledException || exception instanceof LockedException)) {
            handleFailedLoginAttempt(session, remoteAddr);
        }

        // Verificar si la IP está bloqueada por demasiados intentos
        if (isIpBlocked(session)) {
            errorMessage = "Demasiados intentos fallidos. Cuenta temporalmente bloqueada por " +
                          LOCKOUT_DURATION_MINUTES + " minutos.";
            log.warn("IP bloqueada por demasiados intentos: {}", remoteAddr);
        }

        // Añadir el mensaje de error a la sesión
        session.setAttribute("loginError", errorMessage);
        session.setAttribute("attemptedUsername", username);

        // Redireccionar con el error
        setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }

    private String determineErrorMessage(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return "Nombre de usuario o contraseña incorrectos.";
        } else if (exception instanceof DisabledException) {
            return "Tu cuenta no ha sido activada. Verifica tu email para confirmar la cuenta.";
        } else if (exception instanceof LockedException) {
            return "Tu cuenta ha sido bloqueada temporalmente.";
        } else {
            return "Error de autenticación. Inténtalo de nuevo.";
        }
    }

    private void handleFailedLoginAttempt(HttpSession session, String remoteAddr) {
        Integer attempts = (Integer) session.getAttribute("failedLoginAttempts");
        if (attempts == null) {
            attempts = 0;
        }

        attempts++;
        session.setAttribute("failedLoginAttempts", attempts);
        session.setAttribute("lastFailedAttempt", LocalDateTime.now());
        session.setAttribute("failedIpAddress", remoteAddr);

        log.debug("IP {} tiene {} intentos fallidos", remoteAddr, attempts);

        // Si excede el límite, bloquear temporalmente
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            session.setAttribute("ipBlockedUntil",
                LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));

            log.warn("IP {} bloqueada por {} minutos debido a {} intentos fallidos",
                    remoteAddr, LOCKOUT_DURATION_MINUTES, attempts);
        }
    }

    private boolean isIpBlocked(HttpSession session) {
        LocalDateTime blockedUntil = (LocalDateTime) session.getAttribute("ipBlockedUntil");

        if (blockedUntil != null) {
            if (LocalDateTime.now().isBefore(blockedUntil)) {
                return true;
            } else {
                // El bloqueo ha expirado, limpiar
                session.removeAttribute("ipBlockedUntil");
                session.removeAttribute("failedLoginAttempts");
                session.removeAttribute("lastFailedAttempt");
            }
        }

        return false;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}
