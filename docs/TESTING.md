# 🧪 Guía de Testing y Debugging - AfterFilm

Esta guía proporciona información completa sobre cómo realizar testing, debugging y troubleshooting en AfterFilm.

## 🎯 Estrategias de Testing

### Tipos de Tests Implementados

#### 1. Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class MovieServiceTest {
    
    @Mock
    private MovieRepository movieRepository;
    
    @InjectMocks
    private MovieService movieService;
    
    @Test
    void shouldReturnMovieDetails() {
        // Given
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");
        
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        
        // When
        MovieDetailsDTO result = movieService.getCombinedByMovieId(1L);
        
        // Then
        assertThat(result.getTitle()).isEqualTo("Test Movie");
    }
}
```

#### 2. Integration Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiControllerIntegrationTest {
    
    @Container
    static MySQL8Container mysql = new MySQL8Container("mysql:8.0")
            .withDatabaseName("test_pelisapp")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void shouldReturnMoviesList() throws Exception {
        mockMvc.perform(get("/api/movies"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isArray());
    }
}
```

#### 3. API Tests
```bash
# Test de health check
curl -X GET http://localhost:8080/api/health
# Expected: {"status": "ok"}

# Test de autenticación
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "password"}'

# Test de creación de reseña
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"userId": 1, "movieId": 1, "text": "Great movie!", "stars": 5}'
```

---

## 🔍 Debugging

### Configuración de Logging

#### application-dev.properties
```properties
# Debug logging para desarrollo
logging.level.alicanteweb.pelisapp=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Output de logs
logging.file.name=logs/pelisapp.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %level - %msg%n
```

### Debugging por Componente

#### 1. Debugging de Controladores
```java
@Slf4j
@RestController
public class ApiController {
    
    @GetMapping("/api/movies/{id}/details")
    public MovieDetailsDTO getMovieDetails(@PathVariable Long id) {
        log.debug("🔍 Solicitando detalles para película ID: {}", id);
        
        try {
            MovieDetailsDTO result = movieService.getCombinedByMovieId(id);
            log.debug("✅ Detalles obtenidos: {}", result.getTitle());
            return result;
        } catch (Exception e) {
            log.error("❌ Error obteniendo detalles de película {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
```

#### 2. Debugging de Servicios
```java
@Slf4j
@Service
public class AuthService {
    
    public LoginResponse login(LoginRequest request) {
        log.debug("🚪 Intento de login para usuario: {}", request.getUsername());
        
        // Validación de usuario
        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("⚠️ Usuario no encontrado: {}", request.getUsername());
                    return new BadCredentialsException("Invalid credentials");
                });
        
        log.debug("👤 Usuario encontrado: {}, activo: {}", user.getEmail(), user.isActive());
        
        // Resto del método...
    }
}
```

#### 3. Debugging de Base de Datos
```properties
# Mostrar queries SQL
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Mostrar parámetros de binding
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

---

## 🚨 Troubleshooting Común

### Problemas de Base de Datos

#### Error: "Access denied for user"
```bash
# Verificar conexión MySQL
mysql -u pelisapp_user -p -h localhost

# Verificar permisos
SHOW GRANTS FOR 'pelisapp_user'@'localhost';
```

**Solución:**
```sql
GRANT ALL PRIVILEGES ON pelisapp.* TO 'pelisapp_user'@'localhost';
FLUSH PRIVILEGES;
```

#### Error: "Unknown database 'pelisapp'"
```sql
CREATE DATABASE pelisapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Problemas de Autenticación

#### Error: "JWT token expired"
```java
// Verificar configuración de expiración
app.jwt.expiration=86400000  # 24 horas en milisegundos
```

#### Error: "Invalid JWT signature"
```bash
# Verificar que JWT_SECRET esté configurado
echo $JWT_SECRET

# O en application.properties
app.jwt.secret=mySecretKey123456789012345678901234567890
```

### Problemas de TMDB

#### Error: "TMDB API key invalid"
```bash
# Verificar token TMDB
curl -H "Authorization: Bearer YOUR_TOKEN" \
     "https://api.themoviedb.org/3/movie/550"
```

#### Error: "Rate limit exceeded"
```java
// Implementar retry con backoff
@Retryable(value = {Exception.class}, maxAttempts = 3)
public JsonNode getMovieDetails(Long movieId) {
    // TMDB API call
}
```

### Problemas de Email

#### Error: "Authentication failed"
```properties
# Gmail requiere app password
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password  # No la contraseña normal

# Verificar configuración SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.auth=true
```

---

## 📊 Health Checks y Monitoreo

### Health Check Endpoints

```bash
# Health check básico
curl http://localhost:8080/api/health

# Verificar base de datos
curl http://localhost:8080/api/admin/debug/database

# Verificar TMDB connection
curl http://localhost:8080/api/admin/tmdb/test \
  -H "Authorization: Bearer <admin_token>"
```

### Métricas de Aplicación

```java
@Component
public class ApplicationMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleMovieViewed(MovieViewedEvent event) {
        Counter.builder("movies.viewed")
               .tag("movie_id", String.valueOf(event.getMovieId()))
               .register(meterRegistry)
               .increment();
    }
}
```

---

## 🛠️ Herramientas de Debugging

### 1. Logs Estructurados
```java
@Slf4j
public class StructuredLogging {
    
    public void logUserAction(String userId, String action, String resource) {
        log.info("user_action user_id={} action={} resource={}", userId, action, resource);
    }
}
```

### 2. Debug de Requests HTTP
```properties
# Para debugging detallado de HTTP
logging.level.org.springframework.web=DEBUG
logging.level.org.apache.http=DEBUG
```

### 3. Profiling de Performance
```java
@Profile("dev")
@Component
public class PerformanceProfiler {
    
    @EventListener
    @Async
    public void logSlowQueries(ApplicationEvent event) {
        // Log queries que toman más de 1 segundo
    }
}
```

### 4. Database Query Logging
```properties
# Para ver queries lentas
spring.jpa.properties.hibernate.generate_statistics=true
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=1000
```

---

## 🧪 Testing de Endpoints

### Script de Testing Automatizado
```bash
#!/bin/bash
# test-api.sh

BASE_URL="http://localhost:8080"

echo "🧪 Testing AfterFilm API..."

# Test health check
echo "1. Testing health check..."
health_response=$(curl -s ${BASE_URL}/api/health)
echo "Health: $health_response"

# Test movies endpoint
echo "2. Testing movies list..."
movies_response=$(curl -s ${BASE_URL}/api/movies | jq length)
echo "Movies count: $movies_response"

# Test login
echo "3. Testing user login..."
login_response=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

token=$(echo $login_response | jq -r '.accessToken')
echo "Token obtained: ${token:0:20}..."

# Test protected endpoint
echo "4. Testing protected endpoint..."
protected_response=$(curl -s ${BASE_URL}/api/admin/users \
  -H "Authorization: Bearer $token")
echo "Protected endpoint: OK"

echo "✅ All tests completed!"
```

### Postman Collection
```json
{
  "info": {
    "name": "AfterFilm API Tests",
    "description": "Complete test suite for AfterFilm"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/api/health",
          "host": ["{{baseUrl}}"],
          "path": ["api", "health"]
        }
      }
    },
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"admin\",\n  \"password\": \"admin123\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/auth/login",
          "host": ["{{baseUrl}}"],
          "path": ["api", "auth", "login"]
        }
      }
    }
  ]
}
```

---

## 📋 Checklist de Debugging

### Antes de Reportar un Bug
- [ ] Verificar logs de aplicación
- [ ] Comprobar configuración de variables de entorno
- [ ] Validar conectividad de base de datos
- [ ] Verificar permisos de usuario
- [ ] Comprobar formato de requests
- [ ] Revisar documentación de API
- [ ] Intentar reproducir en entorno limpio

### Para Issues de Performance
- [ ] Activar logging de queries SQL
- [ ] Monitorear uso de memoria
- [ ] Verificar índices de base de datos
- [ ] Comprobar tamaño de responses
- [ ] Revisar configuración de cache
- [ ] Analizar tiempo de respuesta de APIs externas

---

Esta guía debe ser tu primera referencia para debugging y troubleshooting en AfterFilm. Mantén esta documentación actualizada según surjan nuevos problemas y soluciones.
