package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.service.UserRegistrationService;
import alicanteweb.pelisapp.service.EmailConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserRegistrationService registrationService;
    private final EmailConfirmationService emailConfirmationService;
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "confirmed", required = false) String confirmed,
                        Model model) {
        if (error != null) model.addAttribute("error", true);
        if (logout != null) model.addAttribute("logout", true);
        if (confirmed != null) model.addAttribute("confirmed", true);
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               @RequestParam String confirmPassword,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            UserRegistrationService.UserRegistrationRequest regRequest = new UserRegistrationService.UserRegistrationRequest(
                    user.getUsername(), user.getEmail(), user.getPassword(), confirmPassword, user.getDisplayName());
            UserRegistrationService.UserRegistrationResult regResult = registrationService.registerUser(regRequest);
            if (!regResult.success()) {
                model.addAttribute("error", regResult.message());
                model.addAttribute("user", user);
                return "register";
            }
            if (emailEnabled) {
                redirectAttributes.addFlashAttribute("success",
                    "✅ Usuario registrado exitosamente. Te hemos enviado un email de confirmación a " + user.getEmail());
            } else {
                redirectAttributes.addFlashAttribute("success",
                    "✅ Usuario registrado exitosamente. El email está deshabilitado, tu cuenta ya está activa.");
            }
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Error interno del servidor: " + e.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }

    @GetMapping("/confirm-email")
    public String confirmEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            EmailConfirmationService.EmailConfirmationResult result = emailConfirmationService.confirmAccount(token);
            if (result.success()) {
                redirectAttributes.addFlashAttribute("confirmed", true);
                redirectAttributes.addFlashAttribute("success", result.message());
            } else {
                redirectAttributes.addFlashAttribute("error", result.message());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error confirmando el email: " + e.getMessage());
        }
        return "redirect:/login";
    }
}
