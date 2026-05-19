# 👨‍💻 Guía para Desarrolladores - AfterFilm

Esta guía proporciona toda la información necesaria para desarrollar, contribuir y extender AfterFilm de manera efectiva.

## 🎯 Preparación del Entorno

### Requisitos del Desarrollador
```bash
# Herramientas necesarias
- Java 17+ (OpenJDK recomendado)
- Maven 3.9+
- MySQL 8.0+
- Git
- IDE (IntelliJ IDEA recomendado)

# Herramientas opcionales útiles
- Postman (testing API)
- MySQL Workbench (gestión BD)
- Docker (containers)
- Ollama (moderación IA)
```

### Setup del Proyecto
```bash
# Clonar y configurar
git clone [url-del-repositorio]
cd AfterFilm

# Configurar IDE (IntelliJ IDEA)
# 1. Abrir proyecto
# 2. Trust Maven project
# 3. Enable annotation processing (Lombok)
# 4. Set Project SDK to Java 17

# Instalar dependencias
mvn clean install

# Ejecutar tests
mvn test

# Ejecutar aplicación
mvn spring-boot:run
```

## 🏗️ Estructura de Desarrollo

### Convenciones de Código

#### Naming Conventions
```java
// Clases: PascalCase
public class MovieService { }
public class UserRegistrationService { }

// Métodos y variables: camelCase
public void createMovie() { }
private String userName;

// Constantes: UPPER_SNAKE_CASE
public static final String DEFAULT_ROLE = "USER";
public static final int MAX_REVIEW_LENGTH = 2000;

// Packages: lowercase con puntos
alicanteweb.pelisapp.service
alicanteweb.pelisapp.dto
```

#### Anotaciones Lombok
```java
@Entity
@Table(name = "movie")
@Getter @Setter          // Getters y setters automáticos
@NoArgsConstructor       // Constructor vacío
@AllArgsConstructor      // Constructor con todos los argumentos
public class Movie {
    // Solo campos, Lombok genera el resto
}

// Para servicios
@Service
@RequiredArgsConstructor // Constructor con campos final
@Slf4j                  // Logger automático
public class MovieService {
    private final MovieRepository movieRepository;
    // log.info() disponible automáticamente
}
```

### Arquitectura de Capas

#### 1. Controllers (Capa de Presentación)
```java
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {
    
    private final MovieService movieService;
    
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailsDTO> getMovie(@PathVariable Long id) {
        // 1. Validar entrada
        // 2. Llamar al servicio
        // 3. Convertir respuesta
        // 4. Manejar excepciones
        
        MovieDetailsDTO movie = movieService.getMovieDetails(id);
        return ResponseEntity.ok(movie);
    }
}
```

#### 2. Services (Capa de Lógica de Negocio)
```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final TMDBClient tmdbClient;
    
    public MovieDetailsDTO getMovieDetails(Long id) {
        // 1. Buscar en BD local
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + id));
        
        // 2. Enriquecer con datos externos si es necesario
        // 3. Aplicar lógica de negocio
        // 4. Convertir a DTO
        
        return convertToDTO(movie);
    }
    
    // Métodos privados para lógica interna
    private MovieDetailsDTO convertToDTO(Movie movie) {
        // Conversión entidad -> DTO
    }
}
```

#### 3. Repositories (Capa de Persistencia)
```java
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    // Query methods automáticos
    List<Movie> findByTitleContainingIgnoreCase(String title);
    Optional<Movie> findByTmdbId(Long tmdbId);
    
    // Queries personalizados
    @Query("SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :start AND :end")
    List<Movie> findByReleaseDateBetween(
        @Param("start") LocalDate start, 
        @Param("end") LocalDate end
    );
    
    // Queries nativos cuando sea necesario
    @Query(value = "SELECT * FROM movie WHERE title MATCH :searchTerm", 
           nativeQuery = true)
    List<Movie> findByFullTextSearch(@Param("searchTerm") String searchTerm);
}
```

### Patrones de Diseño Utilizados

#### 1. Repository Pattern
```java
// Interface genérica
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // Métodos comunes para todas las entidades
}

// Implementación específica
@Repository
public interface MovieRepository extends BaseRepository<Movie, Long> {
    // Métodos específicos de Movie
}
```

#### 2. DTO Pattern
```java
// DTO para transferir datos
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MovieDetailsDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate releaseDate;
    private List<ActorDTO> cast;
    private List<ReviewDTO> reviews;
    
    // Constructor desde entidad
    public static MovieDetailsDTO from(Movie movie) {
        return new MovieDetailsDTO(
            movie.getId(),
            movie.getTitle(),
            movie.getDescription(),
            movie.getReleaseDate(),
            movie.getActors().stream()
                .map(ActorDTO::from)
                .toList(),
            movie.getReviews().stream()
                .map(ReviewDTO::from)
                .toList()
        );
    }
}
```

#### 3. Builder Pattern
```java
// Para construcción compleja de objetos
@Builder
@Getter
public class TMDBMovieRequest {
    private String title;
    private Integer year;
    private String language;
    private Boolean includeAdult;
    
    // Usage:
    TMDBMovieRequest request = TMDBMovieRequest.builder()
        .title("Fight Club")
        .year(1999)
        .language("en")
        .includeAdult(false)
        .build();
}
```

## 🔧 Desarrollo de Características

### Agregar Nueva Entidad

#### 1. Crear Entity
```java
@Entity
@Table(name = "genre")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Genre {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToMany(mappedBy = "genres")
    private Set<Movie> movies = new HashSet<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### 2. Crear Repository
```java
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByNameIgnoreCase(String name);
    List<Genre> findByNameContainingIgnoreCase(String searchTerm);
    
    @Query("SELECT g FROM Genre g WHERE SIZE(g.movies) > :minMovies")
    List<Genre> findGenresWithMinimumMovies(@Param("minMovies") int minMovies);
}
```

#### 3. Crear DTO
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GenreDTO {
    private Long id;
    private String name;
    private String description;
    private Integer movieCount;
    
    public static GenreDTO from(Genre genre) {
        return GenreDTO.builder()
            .id(genre.getId())
            .name(genre.getName())
            .description(genre.getDescription())
            .movieCount(genre.getMovies().size())
            .build();
    }
}
```

#### 4. Crear Service
```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GenreService {
    
    private final GenreRepository genreRepository;
    
    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll().stream()
            .map(GenreDTO::from)
            .toList();
    }
    
    public GenreDTO createGenre(String name, String description) {
        // Validar que no existe
        if (genreRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("Genre already exists: " + name);
        }
        
        Genre genre = Genre.builder()
            .name(name)
            .description(description)
            .build();
        
        Genre saved = genreRepository.save(genre);
        log.info("Created new genre: {} (ID: {})", saved.getName(), saved.getId());
        
        return GenreDTO.from(saved);
    }
}
```

#### 5. Agregar a Controller
```java
@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {
    
    private final GenreService genreService;
    
    @GetMapping
    public ResponseEntity<List<GenreDTO>> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenreDTO> createGenre(@Valid @RequestBody CreateGenreRequest request) {
        GenreDTO created = genreService.createGenre(request.getName(), request.getDescription());
        return ResponseEntity.status(201).body(created);
    }
}
```

### Integración con APIs Externas

#### Cliente HTTP con WebFlux
```java
@Component
@RequiredArgsConstructor
public class TMDBClient {
    
    private final WebClient webClient;
    
    @Value("${app.tmdb.bearer-token}")
    private String bearerToken;
    
    public Mono<TMDBMovieResponse> getMovieDetails(Long movieId) {
        return webClient
            .get()
            .uri("/movie/{id}", movieId)
            .header("Authorization", "Bearer " + bearerToken)
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::handleError)
            .bodyToMono(TMDBMovieResponse.class)
            .doOnSuccess(response -> log.debug("TMDB response for movie {}: {}", movieId, response))
            .doOnError(error -> log.error("TMDB API error for movie {}: {}", movieId, error.getMessage()));
    }
    
    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .map(body -> new ExternalApiException("TMDB API error: " + body));
    }
}
```

### Sistema de Cache

#### Configurar Cache en Service
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {
    
    @Cacheable(value = "movieDetails", key = "#movieId")
    public MovieDetailsDTO getMovieDetails(Long movieId) {
        log.info("Loading movie details from database for ID: {}", movieId);
        // Lógica que se cachea
        return movieDetailsFromDatabase(movieId);
    }
    
    @CacheEvict(value = "movieDetails", key = "#movieId")
    public void updateMovie(Long movieId, UpdateMovieRequest request) {
        log.info("Updating movie and evicting cache for ID: {}", movieId);
        // Actualizar película
    }
    
    @Caching(evict = {
        @CacheEvict(value = "movieDetails", allEntries = true),
        @CacheEvict(value = "movieList", allEntries = true)
    })
    public void clearMovieCache() {
        log.info("Clearing all movie caches");
    }
}
```

### Validaciones Personalizadas

#### Crear Validador Custom
```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {
    String message() default "Email ya está en uso";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, RegisterRequest> {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request.getEmail() == null) return true; // Deja que @NotNull maneje esto
        
        boolean exists = userRepository.findByEmail(request.getEmail()).isPresent();
        
        if (exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "El email '" + request.getEmail() + "' ya está registrado"
            ).addConstraintViolation();
        }
        
        return !exists;
    }
}
```

#### Usar en DTOs
```java
@Getter @Setter
@UniqueEmail
public class RegisterRequest {
    
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe tener formato válido")
    private String email;
    
    @NotBlank(message = "Password es requerido")
    @Size(min = 8, message = "Password debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password debe contener mayúsculas, minúsculas y números")
    private String password;
}
```

## 🧪 Testing

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class MovieServiceTest {
    
    @Mock
    private MovieRepository movieRepository;
    
    @Mock
    private TMDBClient tmdbClient;
    
    @InjectMocks
    private MovieService movieService;
    
    @Test
    void getMovieDetails_WhenMovieExists_ReturnsMovieDetailsDTO() {
        // Given
        Long movieId = 1L;
        Movie movie = Movie.builder()
            .id(movieId)
            .title("Test Movie")
            .description("Test Description")
            .build();
        
        when(movieRepository.findById(movieId))
            .thenReturn(Optional.of(movie));
        
        // When
        MovieDetailsDTO result = movieService.getMovieDetails(movieId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(movieId);
        assertThat(result.getTitle()).isEqualTo("Test Movie");
        verify(movieRepository).findById(movieId);
    }
    
    @Test
    void getMovieDetails_WhenMovieNotExists_ThrowsResourceNotFoundException() {
        // Given
        Long movieId = 999L;
        when(movieRepository.findById(movieId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> movieService.getMovieDetails(movieId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Movie not found: " + movieId);
    }
}
```

### Integration Tests
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MovieControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void getMovieDetails_ReturnsMovieWithCorrectData() {
        // Given - datos insertados por test-data.sql
        Long movieId = 1L;
        
        // When
        ResponseEntity<MovieDetailsDTO> response = restTemplate
            .getForEntity("/api/movies/{id}/details", MovieDetailsDTO.class, movieId);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        MovieDetailsDTO movie = response.getBody();
        assertThat(movie).isNotNull();
        assertThat(movie.getId()).isEqualTo(movieId);
        assertThat(movie.getTitle()).isEqualTo("Test Movie");
    }
}
```

### Test Configuration
```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Disable external integrations in tests
app.tmdb.load-on-startup=false
app.email.enabled=false
app.moderation.enabled=false
```

## 🚀 Deployment

### Perfiles de Configuración

#### application-prod.properties
```properties
# Configuración de producción
server.port=${PORT:8080}
spring.profiles.active=prod

# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JWT (OBLIGATORIO cambiar en producción)
app.jwt.secret=${JWT_SECRET}

# TMDB
app.tmdb.api-key=${TMDB_API_KEY}
app.tmdb.bearer-token=${TMDB_BEARER_TOKEN}

# Email
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# Logging optimizado
logging.level.root=WARN
logging.level.alicanteweb.pelisapp=INFO
spring.jpa.show-sql=false

# Security headers
server.servlet.session.tracking-modes=cookie
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
```

### Docker Setup
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

# Crear directorio de trabajo
WORKDIR /app

# Copiar archivos Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Instalar dependencias (capa separada para cache)
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src src

# Compilar aplicación
RUN ./mvnw clean package -DskipTests

# Exponer puerto
EXPOSE 8080

# Comando de inicio
CMD ["java", "-jar", "target/AfterFilm-0.0.1-SNAPSHOT.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:mysql://db:3306/pelisapp
      - DB_USERNAME=pelisapp
      - DB_PASSWORD=pelisapp123
      - JWT_SECRET=${JWT_SECRET}
      - TMDB_API_KEY=${TMDB_API_KEY}
    depends_on:
      - db
    volumes:
      - ./data:/app/data

  db:
    image: mysql:8.0
    environment:
      - MYSQL_DATABASE=pelisapp
      - MYSQL_USER=pelisapp
      - MYSQL_PASSWORD=pelisapp123
      - MYSQL_ROOT_PASSWORD=root123
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql

volumes:
  db_data:
```

## 🔍 Debugging y Troubleshooting

### Logging Estratégico
```java
@Slf4j
public class MovieService {
    
    public MovieDetailsDTO getMovieDetails(Long movieId) {
        log.debug("🔍 Getting movie details for ID: {}", movieId);
        
        try {
            Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> {
                    log.warn("⚠️ Movie not found: {}", movieId);
                    return new ResourceNotFoundException("Movie not found: " + movieId);
                });
            
            log.info("✅ Found movie: {} ({})", movie.getTitle(), movieId);
            
            MovieDetailsDTO result = convertToDTO(movie);
            log.debug("📤 Returning movie details: {} fields populated", 
                     result.getCast().size() + result.getReviews().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error getting movie details for ID {}: {}", movieId, e.getMessage(), e);
            throw e;
        }
    }
}
```

### Actuator Endpoints
```properties
# En application.properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

```java
// Custom Health Indicator
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public Health health() {
        try {
            long userCount = userRepository.count();
            return Health.up()
                .withDetail("userCount", userCount)
                .withDetail("status", "Database is accessible")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Common Issues y Soluciones

#### 1. LazyInitializationException
```java
// ❌ Problemático
@Transactional
public MovieDetailsDTO getMovieDetails(Long id) {
    Movie movie = movieRepository.findById(id).orElseThrow();
    // ❌ Falla si intentas acceder fuera de la transacción
    return convertToDTO(movie); // Si convertToDTO accede a movie.getReviews()
}

// ✅ Solución: Eager loading específico
@Query("SELECT m FROM Movie m LEFT JOIN FETCH m.reviews LEFT JOIN FETCH m.actors WHERE m.id = :id")
Optional<Movie> findByIdWithDetails(@Param("id") Long id);
```

#### 2. N+1 Query Problem
```java
// ❌ Problemático (genera N+1 queries)
public List<MovieListDTO> getAllMovies() {
    List<Movie> movies = movieRepository.findAll();
    return movies.stream()
        .map(movie -> MovieListDTO.builder()
            .title(movie.getTitle())
            .reviewCount(movie.getReviews().size()) // Query para cada película
            .build())
        .toList();
}

// ✅ Solución: Batch loading o JOIN FETCH
@Query("SELECT m FROM Movie m LEFT JOIN FETCH m.reviews")
List<Movie> findAllWithReviews();
```

### Performance Monitoring

```java
// Interceptor para medir tiempos de respuesta
@Component
public class PerformanceInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("🕒 {} {} completed in {}ms", request.getMethod(), request.getRequestURI(), duration);
            
            if (duration > 1000) {
                log.warn("⚠️ Slow request detected: {} {} took {}ms", 
                        request.getMethod(), request.getRequestURI(), duration);
            }
        }
    }
}
```

## 📚 Recursos y Referencias

### Documentación Oficial
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

### Guías Útiles
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [JWT with Spring Security](https://www.baeldung.com/spring-security-jwt)
- [JPA Best Practices](https://vladmihalcea.com/jpa-hibernate-performance-tuning/)

### Herramientas Recomendadas
- **IDE**: IntelliJ IDEA Ultimate
- **Database**: MySQL Workbench
- **API Testing**: Postman, Thunder Client
- **Monitoring**: Spring Boot Actuator + Micrometer
- **Profiling**: JProfiler, VisualVM

---

¡Feliz codificación! 🚀 Si tienes dudas, consulta la documentación o contacta al equipo de desarrollo.
