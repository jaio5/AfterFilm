package alicanteweb.pelisapp.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler personalizado para login exitoso
 * Implementa lógica adicional de seguridad y logging
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws ServletException, IOException {

        String username = authentication.getName();
        String remoteAddr = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // Log de seguridad
        log.info("Login correcto: usuario={}, ip={}, userAgent={}",
                username, remoteAddr, userAgent);

        // Limpiar intentos de login fallidos (si los hay)
        clearFailedAttempts(request);

        // Verificar si hay una URL guardada para redireccionar
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            log.debug("Redirigiendo a URL guardada: {}", targetUrl);

            // Validar que la URL de redirección es segura
            if (isValidRedirectUrl(targetUrl)) {
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }

        // Redirección por defecto basada en roles
        String redirectUrl = determineTargetUrl(authentication);
        log.debug("Redirigiendo a URL por defecto: {}", redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return "/admin";
        }

        // Para usuarios normales, ir al inicio
        return "/";
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

    private void clearFailedAttempts(HttpServletRequest request) {
        // Limpiar contadores de intentos fallidos de la sesión
        request.getSession().removeAttribute("failedLoginAttempts");
        request.getSession().removeAttribute("lastFailedAttempt");
    }

    private boolean isValidRedirectUrl(String url) {
        // Prevenir ataques de redirección abierta
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Solo permitir URLs relativas o del mismo dominio
        return url.startsWith("/") && !url.startsWith("//");
    }
}
