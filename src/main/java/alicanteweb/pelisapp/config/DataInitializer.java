package alicanteweb.pelisapp.config;

import alicanteweb.pelisapp.constants.RoleConstants;
import alicanteweb.pelisapp.entity.Role;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.RoleRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Inicializador de datos para desarrollo y pruebas.
 * Crea usuario admin automáticamente si no existe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-password:}")
    private String adminInitialPassword;

    @Override
    public void run(String... args) throws Exception {
        // Crear roles necesarios
        createRoles();

        // Crear o actualizar usuario admin
        User existingAdmin = userRepository.findByUsername("admin").orElse(null);
        if (existingAdmin != null) {
            updateAdminUser(existingAdmin);
        } else {
            createAdminUser();
        }
    }

    private void createRoles() {
        createRoleIfNotExists(RoleConstants.ADMIN, "Administrador del sistema");
        createRoleIfNotExists(RoleConstants.USER, "Usuario regular");
        createRoleIfNotExists(RoleConstants.MODERATOR, "Moderador de contenido");
        createRoleIfNotExists(RoleConstants.SUPERADMIN, "Super administrador");
    }

    private Role createRoleIfNotExists(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void updateAdminUser(User admin) {
        log.info("Actualizando usuario administrador...");

        if (adminInitialPassword != null && !adminInitialPassword.isBlank()) {
            admin.setPassword(passwordEncoder.encode(adminInitialPassword));
            log.warn("Contraseña del admin actualizada desde app.admin.initial-password");
        }
        admin.setEmailConfirmed(true); // Admin siempre confirmado
        admin.setDisplayName("Administrador");
        admin.setEmail("admin@pelisapp.com");

        // Asegurar que tenga todos los roles de admin
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(createRoleIfNotExists(RoleConstants.ADMIN, "Administrador del sistema"));
        adminRoles.add(createRoleIfNotExists(RoleConstants.MODERATOR, "Moderador de contenido"));
        adminRoles.add(createRoleIfNotExists(RoleConstants.SUPERADMIN, "Super administrador"));
        admin.setRoles(adminRoles);

        userRepository.save(admin);

        log.info("Usuario admin actualizado exitosamente (username: admin, email: admin@pelisapp.com)");
    }

    private void createAdminUser() {
        log.info("Creando usuario administrador...");

        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            log.warn("No se crea usuario admin inicial porque APP_ADMIN_PASSWORD/app.admin.initial-password no esta definido");
            return;
        }

        // Crear usuario admin
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@pelisapp.com");
        admin.setPassword(passwordEncoder.encode(adminInitialPassword));
        admin.setDisplayName("Administrador");
        admin.setRegisteredAt(Instant.now());
        admin.setEmailConfirmed(true); // Admin confirmado automáticamente
        admin.setCriticLevel(0);

        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(createRoleIfNotExists(RoleConstants.ADMIN, "Administrador del sistema"));
        adminRoles.add(createRoleIfNotExists(RoleConstants.MODERATOR, "Moderador de contenido"));
        adminRoles.add(createRoleIfNotExists(RoleConstants.SUPERADMIN, "Super administrador"));
        admin.setRoles(adminRoles);

        userRepository.save(admin);

        log.info("Usuario admin creado exitosamente (username: admin, email: admin@pelisapp.com, roles: ADMIN, MODERATOR, SUPERADMIN)");
    }

}
