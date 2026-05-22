package alicanteweb.pelisapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Servicio de email usando la API HTTP de Resend.
 */
@Service("resendEmailService")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailService implements IEmailService {

    private final RestTemplate restTemplate;

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.api.url:https://api.resend.com/emails}")
    private String apiUrl;

    @Value("${spring.mail.from:${MAIL_FROM:}}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.name:AfterFilm}")
    private String appName;

    @PostConstruct
    public void init() {
        log.info("ResendEmailService inicializado. From: {}, API key: {}",
            isBlank(fromEmail) ? "NO CONFIGURADO" : fromEmail,
            isBlank(apiKey) ? "NO CONFIGURADA" : "CONFIGURADA");
    }

    @Override
    public void sendConfirmationEmail(String toEmail, String username, String confirmationToken) {
        validateConfiguration();

        String confirmationUrl = baseUrl + "/confirm-email?token=" + confirmationToken;
        String htmlContent = createConfirmationEmailHTML(username, confirmationUrl);

        Map<String, Object> payload = Map.of(
            "from", fromEmail,
            "to", List.of(toEmail),
            "subject", "Confirma tu cuenta en " + appName,
            "html", htmlContent
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            log.info("Enviando email de confirmación con Resend a: {} desde: {}", toEmail, fromEmail);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class
            );

            HttpStatusCode status = response.getStatusCode();
            if (!status.is2xxSuccessful()) {
                throw new RuntimeException("Resend respondió con estado " + status.value());
            }

            log.info("Email de confirmación enviado con Resend a: {}", toEmail);
        } catch (RestClientResponseException e) {
            log.error("Error Resend enviando email a {}: HTTP {} - {}", toEmail, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error Resend HTTP " + e.getRawStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error inesperado enviando email con Resend a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email con Resend: " + e.getMessage(), e);
        }
    }

    private void validateConfiguration() {
        if (isBlank(apiKey)) {
            throw new RuntimeException("RESEND_API_KEY no configurada");
        }
        if (isBlank(fromEmail)) {
            throw new RuntimeException("MAIL_FROM o RESEND_FROM no configurado");
        }
        if (isBlank(apiUrl)) {
            throw new RuntimeException("RESEND_API_URL no configurada");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String createConfirmationEmailHTML(String username, String confirmationUrl) {
        String imageUrl = baseUrl + "/logo.png";
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Confirma tu cuenta en %s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f9fa;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1);">
                                <tr>
                                    <td style="padding: 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; border-radius: 10px 10px 0 0;">
                                        <img src="%s" alt="%s" style="height:64px; display:block; margin:0 auto 8px;">
                                        <h1 style="margin: 0; font-size: 22px; font-weight: bold;">%s</h1>
                                        <p style="margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;">Tu red social de peliculas</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="color: #333; margin-top: 0;">Hola %s</h2>
                                        <p style="color: #666; line-height: 1.6; margin: 20px 0;">Gracias por unirte a <strong>%s</strong>. Para completar tu registro, necesitamos confirmar tu direccion de correo electronico.</p>
                                        <div style="text-align: center; margin: 30px 0;">
                                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #ff6b35 0%%, #f7931e 100%%); color: white; text-decoration: none; padding: 15px 30px; border-radius: 25px; font-weight: bold; font-size: 16px;">
                                                Confirmar mi cuenta
                                            </a>
                                        </div>
                                        <p style="color: #666; line-height: 1.6;">Si el boton no funciona, copia y pega este enlace en tu navegador:</p>
                                        <p style="background-color: #f8f9fa; padding: 10px; border-radius: 5px; word-break: break-all; font-family: monospace; font-size: 12px; color: #666;">%s</p>
                                        <p style="color: #999; font-size: 14px; margin-top: 30px; text-align: center;">
                                            Este enlace de confirmacion expira en 24 horas.<br>
                                            Si no creaste esta cuenta, puedes ignorar este email.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px; text-align: center; background-color: #f8f9fa; border-radius: 0 0 10px 10px;">
                                        <p style="margin: 0; color: #999; font-size: 14px;">
                                            El equipo de %s
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, appName, imageUrl, appName, appName, username, appName, confirmationUrl, confirmationUrl, appName);
    }
}
