package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.constants.RoleConstants;
import alicanteweb.pelisapp.entity.Role;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.RoleRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.UserValidationService.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationService validationService;
    private final EmailConfirmationService emailConfirmationService;

    @Transactional
    public UserRegistrationResult registerUser(UserRegistrationRequest request) {
        try {
            ValidationResult validationResult = validateRegistrationData(request);
            if (!validationResult.valid()) {
                return UserRegistrationResult.failure(validationResult.errorMessage());
            }

            if (userRepository.existsByUsername(request.username())) {
                return UserRegistrationResult.failure("El usuario ya existe");
            }
            if (userRepository.existsByEmail(request.email())) {
                return UserRegistrationResult.failure("El email ya esta en uso");
            }

            User savedUser = userRepository.save(createUser(request));
            sendConfirmationEmail(savedUser);

            log.info("Usuario registrado exitosamente: {}", request.username());
            return UserRegistrationResult.success(savedUser, "Usuario registrado exitosamente");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error registrando usuario {}: {}", request.username(), e.getMessage(), e);
            return UserRegistrationResult.failure("Error al crear la cuenta: " + e.getMessage());
        }
    }

    private ValidationResult validateRegistrationData(UserRegistrationRequest request) {
        ValidationResult passwordResult = validationService.validatePassword(request.password());
        if (!passwordResult.valid()) return passwordResult;

        ValidationResult passwordMatchResult = validationService
                .validatePasswordMatch(request.password(), request.confirmPassword());
        if (!passwordMatchResult.valid()) return passwordMatchResult;

        ValidationResult usernameResult = validationService.validateUsername(request.username());
        if (!usernameResult.valid()) return usernameResult;

        ValidationResult emailResult = validationService.validateEmail(request.email());
        if (!emailResult.valid()) return emailResult;

        ValidationResult displayNameResult = validationService.validateDisplayName(request.displayName());
        if (!displayNameResult.valid()) return displayNameResult;

        ValidationResult suspiciousContentResult = validationService.validateSuspiciousContent(request.username());
        if (!suspiciousContentResult.valid()) return suspiciousContentResult;

        return ValidationResult.success();
    }

    private User createUser(UserRegistrationRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(normalizeDisplayName(request.displayName()));
        user.setRegisteredAt(Instant.now());
        user.setEmailConfirmed(false);

        Set<Role> roles = new HashSet<>();
        roles.add(getDefaultUserRole());
        user.setRoles(roles);

        return user;
    }

    private String normalizeDisplayName(String displayName) {
        return displayName == null || displayName.trim().isEmpty() ? null : displayName.trim();
    }

    private Role getDefaultUserRole() {
        return roleRepository.findByName(RoleConstants.USER).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleConstants.USER);
            role.setDescription("Usuario estandar");
            return roleRepository.save(role);
        });
    }

    private void sendConfirmationEmail(User user) {
        String token = emailConfirmationService.generateConfirmationToken(user);
        emailConfirmationService.sendConfirmationEmail(user, token);
    }

    public record UserRegistrationRequest(String username, String email, String password, String confirmPassword,
                                          String displayName) {
    }

    public record UserRegistrationResult(boolean success, User user, String message) {
        public static UserRegistrationResult success(User user, String message) {
            return new UserRegistrationResult(true, user, message);
        }

        public static UserRegistrationResult failure(String message) {
            return new UserRegistrationResult(false, null, message);
        }
    }
}
