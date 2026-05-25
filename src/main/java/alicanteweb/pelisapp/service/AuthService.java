package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.LoginResponse;
import alicanteweb.pelisapp.dto.LoginRequest;
import alicanteweb.pelisapp.dto.RegisterRequest;
import alicanteweb.pelisapp.dto.UserDTO;
import alicanteweb.pelisapp.entity.Role;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.RoleRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Servicio principal de autenticación.
 * Refactorizado aplicando SRP - se enfoca únicamente en autenticación.
 * Responsabilidades movidas:
 * - Validaciones → UserValidationService
 * - Registro → UserRegistrationService
 * - Confirmación email → EmailConfirmationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailConfirmationService emailConfirmationService;
    private final UserProfileImageService userProfileImageService;

    /**
     * Registro API (mantener compatibilidad).
     * NOTA: Para web, usar UserRegistrationService directamente.
     */
    @Transactional
    public LoginResponse register(RegisterRequest req) {
        log.info("Registrando usuario por API: {}", req.getUsername());

        validateRegistrationRequest(req);
        User user = createUserFromRequest(req);
        assignDefaultRole(user);

        userRepository.save(user);
        String confirmationToken = emailConfirmationService.generateConfirmationToken(user);
        emailConfirmationService.sendConfirmationEmail(user, confirmationToken);
        log.info("Usuario registrado exitosamente: {}", req.getUsername());
        // Añadir roles al UserDTO
        java.util.List<String> rolesList = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toList());
        UserDTO userDTO = toUserDTO(user, rolesList);
        return generateTokensForUser(user.getUsername(), Collections.singleton("ROLE_USER"), userDTO);
    }

    private void validateRegistrationRequest(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            log.warn("Username ya existe: {}", req.getUsername());
            throw new IllegalArgumentException("username already exists");
        }
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail())) {
            log.warn("Email ya existe: {}", req.getEmail());
            throw new IllegalArgumentException("email already exists");
        }
    }

    private User createUserFromRequest(RegisterRequest req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setRegisteredAt(Instant.now());
        user.setEmailConfirmed(false);
        return user;
    }

    private void assignDefaultRole(User user) {
        Role defaultRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            log.info("Creando rol por defecto ROLE_USER");
            Role role = new Role();
            role.setName("ROLE_USER");
            role.setDescription("Default role");
            return roleRepository.save(role);
        });

        user.setRoles(new HashSet<>(Collections.singleton(defaultRole)));
    }

    private LoginResponse generateTokensForUser(String username, Set<String> roles, UserDTO userDTO) {
        String access = jwtTokenProvider.createAccessToken(username, roles);
        String refresh = jwtTokenProvider.createRefreshToken(username);
        long expires = jwtTokenProvider.getExpiryMillis(access);
        return new LoginResponse(access, expires, refresh, userDTO);
    }

    private LoginResponse generateTokensForUser(String username, Set<String> roles) {
        // Para compatibilidad, pero sin userDTO
        String access = jwtTokenProvider.createAccessToken(username, roles);
        String refresh = jwtTokenProvider.createRefreshToken(username);
        long expires = jwtTokenProvider.getExpiryMillis(access);
        return new LoginResponse(access, expires, refresh);
    }

    /**
     * Autenticación principal del sistema.
     */
    public LoginResponse login(LoginRequest req) {
        log.info("Intentando login para usuario: {}", req.getUsername());
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

            org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();

            Set<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

            User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
            UserDTO userDTO = null;
            if (user != null) {
                java.util.List<String> rolesList = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toList());
                userDTO = toUserDTO(user, rolesList);
            }
            log.info("Login exitoso para usuario: {}", req.getUsername());
            return generateTokensForUser(principal.getUsername(), roles, userDTO);

        } catch (Exception e) {
            log.error("Error en login para usuario {}: {}", req.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * Renovación de token.
     */
    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(java.util.stream.Collectors.toSet());

        String access = jwtTokenProvider.createAccessToken(username, roles);
        long expires = jwtTokenProvider.getExpiryMillis(access);
        UserDTO userDTO = toUserDTO(user, user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toList()));
        return new LoginResponse(access, expires, refreshToken, userDTO);
    }

    /**
     * Buscar usuario por email (mantenido para compatibilidad).
     */
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Buscar usuario por nombre de usuario (usado por el admin).
     */
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public UserDTO getUserDTOByUsername(String username) {
        User user = findUserByUsername(username);
        if (user == null) return null;
        return toUserDTO(user, user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toList()));
    }

    private UserDTO toUserDTO(User user, java.util.List<String> roles) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                userProfileImageService.profileImageUrl(user),
                user.getCriticLevel(),
                roles);
    }
}
