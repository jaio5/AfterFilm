package alicanteweb.pelisapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio real de email que envía correos reales con Gmail.
 * Se activa cuando app.email.enabled=true
 */
@Service("realEmailService")
@Primary
@Slf4j
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class RealEmailService implements IEmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:#{null}}")
    private String fromEmail;

    @Value("${app.name:PelisApp}")
    private String appName;

    @PostConstruct
    public void init() {
        if (mailSender == null) {
            log.warn("⚠️  JavaMailSender no está disponible. Verifica la configuración de email.");
        } else {
            log.info("✅ RealEmailService inicializado con Gmail: {}", fromEmail);
        }
    }

    @Override
    public void sendConfirmationEmail(String toEmail, String username, String confirmationToken) {
        if (mailSender == null) {
            log.error("❌ JavaMailSender no está configurado. No se puede enviar email.");
            throw new RuntimeException("Servicio de email no configurado");
        }

        if (fromEmail == null || fromEmail.isEmpty()) {
            log.error("❌ spring.mail.username no está configurado.");
            throw new RuntimeException("Email username no configurado");
        }

        try {
            log.info("📧 Enviando email de confirmación a: {} desde: {}", toEmail, fromEmail);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject("🎬 Confirma tu cuenta en " + appName);

            String confirmationUrl = baseUrl + "/confirm-email?token=" + confirmationToken;
            String htmlContent = createConfirmationEmailHTML(username, confirmationUrl);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ Email de confirmación enviado exitosamente a: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Error enviando email de confirmación a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email de confirmación: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error inesperado enviando email: {}", e.getMessage());
            throw new RuntimeException("Error inesperado: " + e.getMessage(), e);
        }
    }

    /**
     * Crea contenido HTML bonito para el email de confirmación
     */
    private String createConfirmationEmailHTML(String username, String confirmationUrl) {
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
                                        <h1 style="margin: 0; font-size: 28px; font-weight: bold;">🎬 %s</h1>
                                        <p style="margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">Tu red social de películas</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="color: #333; margin-top: 0;">¡Hola %s! 👋</h2>
                                        <p style="color: #666; line-height: 1.6; margin: 20px 0;">¡Gracias por unirte a <strong>%s</strong>! Para completar tu registro y comenzar a descubrir películas increíbles, necesitamos confirmar tu dirección de correo electrónico.</p>
                                        <div style="text-align: center; margin: 30px 0;">
                                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #ff6b35 0%%, #f7931e 100%%); color: white; text-decoration: none; padding: 15px 30px; border-radius: 25px; font-weight: bold; font-size: 16px; transition: transform 0.2s;">
                                                ✅ Confirmar mi Cuenta
                                            </a>
                                        </div>
                                        <p style="color: #666; line-height: 1.6;">Si el botón no funciona, también puedes copiar y pegar este enlace en tu navegador:</p>
                                        <p style="background-color: #f8f9fa; padding: 10px; border-radius: 5px; word-break: break-all; font-family: monospace; font-size: 12px; color: #666;">%s</p>
                                        <div style="margin-top: 30px; padding: 20px; background-color: #e3f2fd; border-radius: 8px; border-left: 4px solid #2196f3;">
                                            <p style="margin: 0; color: #1976d2; font-weight: bold;">🔐 ¿Por qué confirmar tu email?</p>
                                            <ul style="margin: 10px 0 0 0; color: #666;">
                                                <li>🛡️ Protege tu cuenta contra accesos no autorizados</li>
                                                <li>📧 Recibe notificaciones importantes sobre tu cuenta</li>
                                                <li>🎬 Mantente al día con nuevas películas y reseñas</li>
                                                <li>💌 Recupera tu cuenta si olvidas tu contraseña</li>
                                            </ul>
                                        </div>
                                        <p style="color: #999; font-size: 14px; margin-top: 30px; text-align: center;">
                                            Este enlace de confirmación expira en 24 horas.<br>
                                            Si no creaste esta cuenta, puedes ignorar este email.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px; text-align: center; background-color: #f8f9fa; border-radius: 0 0 10px 10px;">
                                        <p style="margin: 0; color: #999; font-size: 14px;">
                                            ¡Disfruta descubriendo y valorando películas!<br>
                                            <strong>El equipo de %s</strong> 🎭
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, appName, appName, username, appName, confirmationUrl, confirmationUrl, appName);
    }
}
