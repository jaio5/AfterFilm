package alicanteweb.pelisapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class EmailDiagnosticService {

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.provider:resend}")
    private String emailProvider;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.api.url:https://api.resend.com/emails}")
    private String resendApiUrl;

    @PostConstruct
    public void diagnosticEmailConfiguration() {
        log.info("=== DIAGNOSTICO DE EMAIL ===");
        log.info("Email habilitado: {}", emailEnabled);
        log.info("Provider: {}", emailProvider);
        log.info("Resend API URL: {}", resendApiUrl);
        log.info("Remitente: {}", isBlank(mailFrom) ? "NO CONFIGURADO" : mailFrom);
        log.info("Resend API key configurada: {}", isBlank(resendApiKey) ? "NO" : "SI");
        log.info("=== FIN DIAGNOSTICO DE EMAIL ===");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
