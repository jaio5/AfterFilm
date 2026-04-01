package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailApiController {
    private final IEmailService emailService;

    @PostMapping("/test")
    public ResponseEntity<String> testEmail(@RequestParam String email) {
        try {
            log.info("🧪 Testing email send to: {}", email);
            String testToken = "test-token-" + System.currentTimeMillis();
            emailService.sendConfirmationEmail(email, "TestUser", testToken);
            return ResponseEntity.ok("✅ Email enviado exitosamente a " + email +
                "\n📬 Revisa tu bandeja de entrada y carpeta de SPAM");
        } catch (Exception e) {
            log.error("❌ Error enviando email de prueba: {}", e.getMessage());
            return ResponseEntity.badRequest().body("❌ Error enviando email: " + e.getMessage());
        }
    }
}
