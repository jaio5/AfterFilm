package alicanteweb.pelisapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    public SecurityConfig(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);

        http
            .csrf(AbstractHttpConfigurer::disable)

            // Configuración de CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Control de sesiones básico
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(3)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(new org.springframework.security.core.session.SessionRegistryImpl())
            )

            .authorizeHttpRequests(auth -> {
                // Rutas públicas
                auth.requestMatchers("/", "/login", "/register").permitAll();
                auth.requestMatchers("/confirm-account/**", "/resend-confirmation", "/request-confirmation").permitAll();
                auth.requestMatchers("/api/auth/**").permitAll();
                auth.requestMatchers("/css/**", "/js/**", "/images/**", "/supabase-images/**").permitAll();
                auth.requestMatchers("/pelicula/**").permitAll();
                auth.requestMatchers("/peliculas/**").permitAll();
                auth.requestMatchers("/series", "/serie/**", "/libros", "/libro/**").permitAll();
                auth.requestMatchers("/api/series/**", "/api/books/**").permitAll();
                auth.requestMatchers("/usuarios", "/usuario/**").permitAll();
                auth.requestMatchers("/api/social/**").permitAll();
                auth.requestMatchers("/public/**").permitAll(); // Endpoints públicos para pruebas

                // Endpoints de diagnóstico (solo en desarrollo)
                if (devMode) {
                    auth.requestMatchers("/tmdb/setup", "/test-tmdb-simple", "/diagnostico/**").permitAll();
                    auth.requestMatchers("/admin/users-management/**").permitAll(); // TEMPORAL para gestión de usuarios
                }

                // Rutas administrativas
                auth.requestMatchers("/admin/**").hasRole("ADMIN");

                // Rutas que requieren autenticación
                auth.requestMatchers("/perfil/**", "/profile/**").authenticated();
                auth.requestMatchers("/feed", "/chat", "/chat/**").authenticated();
                auth.requestMatchers("/api/chat/**").authenticated();
                auth.requestMatchers("/api/lists/**").authenticated();
                auth.requestMatchers("/review/**").authenticated();
                auth.requestMatchers("/api/user/**").authenticated();
                auth.requestMatchers(HttpMethod.POST, "/pelicula/*/review").authenticated();
                auth.requestMatchers(HttpMethod.POST, "/review/*/like").authenticated();

                // Por defecto permitir acceso (para desarrollo)
                auth.anyRequest().permitAll();
            })

            // Configuración de login por formulario
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(authenticationSuccessHandler())
                    .failureHandler(authenticationFailureHandler())
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .permitAll()
            )

            // Configuración de logout
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .clearAuthentication(true)
                    .permitAll()
            )

            // Configuración para recordar usuario
            .rememberMe(remember -> remember
                    .key("pelisapp-remember-me-key")
                    .tokenValiditySeconds(86400 * 7) // 7 días
                    .userDetailsService(userDetailsService)
            )

            // Solo añadir JWT filter para rutas de API
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10); // Strength factor estándar para compatibilidad
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite localhost y emuladores Android
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "https://localhost:*",
                "http://10.0.2.2:*",
                "http://192.168.*:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
