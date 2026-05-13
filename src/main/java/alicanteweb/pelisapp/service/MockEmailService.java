package alicanteweb.pelisapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock del servicio de email para desarrollo.
 * Se activa cuando app.email.enabled=false O cuando no hay configuración de mail
 */
@Service("mockEmailService")
@Slf4j
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class MockEmailService implements IEmailService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public void sendConfirmationEmail(String toEmail, String username, String confirmationToken) {
        String confirmationUrl = baseUrl + "/confirm-email?token=" + confirmationToken;

        log.info("📧 MOCK EMAIL - Email de confirmación simulado");
        log.info("   📧 Para: {}", toEmail);
        log.info("   👤 Usuario: {}", username);
        log.info("   🔗 URL de confirmación: {}", confirmationUrl);
        log.info("   🎫 Token: {}...", confirmationToken.substring(0, Math.min(20, confirmationToken.length())));
        log.info("💡 En desarrollo: Haz clic en la URL de arriba para confirmar la cuenta");
    }
}
