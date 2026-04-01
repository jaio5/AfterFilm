package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.LoginRequest;
import alicanteweb.pelisapp.dto.LoginResponse;
import alicanteweb.pelisapp.dto.RegisterRequest;
import alicanteweb.pelisapp.service.AuthService;
import alicanteweb.pelisapp.service.EmailConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {
    private final AuthService authService;
    private final EmailConfirmationService emailConfirmationService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        LoginResponse resp = authService.register(req);
        return ResponseEntity.status(201).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse resp = authService.login(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody Map<String, String> req) {
        String refreshToken = req.get("refreshToken");
        LoginResponse resp = authService.refresh(refreshToken);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<Map<String, Object>> confirmEmail(@RequestParam("token") String token) {
        var result = emailConfirmationService.confirmAccount(token);
        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result.message()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", result.message()
            ));
        }
    }
}
