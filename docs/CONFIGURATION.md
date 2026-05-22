# ⚙️ Guía de Configuración - AfterFilm

Esta documentación detalla todas las opciones de configuración disponibles para personalizar AfterFilm según tus necesidades específicas.

## 🔧 Configuraciones Principales

### 📊 Base de Datos

#### MySQL Configuration
```properties
# Conexión básica
spring.datasource.url=jdbc:mysql://localhost:3306/pelisapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Pool de conexiones (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.leak-detection-threshold=60000

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=${SHOW_SQL:false}
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

#### Configuraciones Avanzadas de Hibernate
```properties
# Cache de segundo nivel
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.caffeine.CaffeineCacheRegionFactory
spring.jpa.properties.hibernate.cache.use_query_cache=true

# Estadísticas (desarrollo)
spring.jpa.properties.hibernate.generate_statistics=${HIBERNATE_STATS:false}
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=100

# Validación
spring.jpa.properties.hibernate.check_nullability=true
spring.jpa.properties.hibernate.validator.apply_to_ddl=true
```

### 🔐 Autenticación y Seguridad

#### JWT Configuration
```properties
# JWT Settings (OBLIGATORIO cambiar en producción)
app.jwt.secret=${APP_JWT_SECRET:changeit_0123456789_changeit_0123456789_32chars}
app.jwt.access-expiration-ms=${APP_JWT_ACCESS_EXPIRATION_MS:86400000}
app.jwt.refresh-expiration-ms=${APP_JWT_REFRESH_EXPIRATION_MS:604800000}

# Backwards compatibility
app.jwt.expiration-ms=${app.jwt.access-expiration-ms}
app.jwt.refresh-ttl-sec=${app.jwt.refresh-expiration-ms}

# Configuración de algoritmo
app.jwt.algorithm=HS256
app.jwt.issuer=${APP_BASE_URL:http://localhost:8080}
```

#### Spring Security
```properties
# Configuración de sesiones
server.servlet.session.tracking-modes=cookie
server.servlet.session.cookie.name=PELISAPP_SESSION
server.servlet.session.cookie.max-age=3600
server.servlet.session.cookie.secure=${COOKIE_SECURE:false}
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict

# CORS Configuration
app.cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:8080}
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=Content-Type,Authorization,X-Requested-With
app.cors.exposed-headers=Authorization
app.cors.allow-credentials=true
app.cors.max-age=3600
```

### 🎬 Integración TMDB

#### TMDB API Configuration
```properties
# URLs y endpoints
app.tmdb.base-url=https://api.themoviedb.org/3
app.tmdb.image-base-url=https://image.tmdb.org/t/p

# Autenticación (usar Bearer Token preferiblemente)
app.tmdb.api-key=${TMDB_API_KEY:your_api_key_here}
app.tmdb.bearer-token=${TMDB_BEARER_TOKEN:your_bearer_token_here}

# Configuración de carga
app.tmdb.load-on-startup=${TMDB_LOAD_STARTUP:true}
app.tmdb.load-pages=${TMDB_LOAD_PAGES:50}
app.tmdb.load-delay-ms=${TMDB_LOAD_DELAY:1000}
app.tmdb.max-retries=${TMDB_MAX_RETRIES:3}

# Cache y timeouts
app.tmdb.cache-duration-hours=${TMDB_CACHE_HOURS:24}
app.tmdb.connection-timeout=${TMDB_TIMEOUT:30000}
app.tmdb.read-timeout=${TMDB_READ_TIMEOUT:30000}

# Configuración de imágenes
app.tmdb.poster-sizes=w92,w154,w185,w342,w500,w780,original
app.tmdb.profile-sizes=w45,w185,h632,original
app.tmdb.backdrop-sizes=w300,w780,w1280,original
app.tmdb.download-images=${TMDB_DOWNLOAD_IMAGES:true}
```

#### Configuración Avanzada TMDB
```properties
# Filtros de contenido
app.tmdb.include-adult=${TMDB_ADULT_CONTENT:false}
app.tmdb.min-vote-average=${TMDB_MIN_RATING:5.0}
app.tmdb.min-vote-count=${TMDB_MIN_VOTES:100}
app.tmdb.language=${TMDB_LANGUAGE:es-ES}
app.tmdb.region=${TMDB_REGION:ES}

# Categorías específicas a cargar
app.tmdb.genres=${TMDB_GENRES:28,12,16,35,80,99,18,10751,14,36,27,10402,9648,10749,878,10770,53,10752,37}

# Rate limiting para API
app.tmdb.requests-per-second=${TMDB_RPS:4}
app.tmdb.burst-capacity=${TMDB_BURST:10}
```

### 📧 Sistema de Email

#### SMTP Configuration (Gmail)
```properties
# Configuración básica
app.email.enabled=${EMAIL_ENABLED:true}
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${SPRING_MAIL_USERNAME:your_email@gmail.com}
spring.mail.password=${SPRING_MAIL_PASSWORD:your_app_password}

# Propiedades SMTP
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=${MAIL_TIMEOUT:10000}
spring.mail.properties.mail.smtp.timeout=${MAIL_TIMEOUT:10000}
spring.mail.properties.mail.smtp.writetimeout=${MAIL_TIMEOUT:10000}

# Configuración SSL/TLS
spring.mail.properties.mail.smtp.ssl.trust=${MAIL_HOST:smtp.gmail.com}
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2

# Configuración adicional
spring.mail.properties.mail.transport.protocol=smtp
spring.mail.properties.mail.smtp.port=${MAIL_PORT:587}
spring.mail.default-encoding=UTF-8
spring.mail.test-connection=${MAIL_TEST_CONNECTION:false}
```

#### Email Templates y Configuración
```properties
# URLs base para enlaces
app.base-url=${APP_BASE_URL:http://localhost:8080}
app.frontend-url=${FRONTEND_URL:http://localhost:3000}

# Configuración de emails
app.email.from=${spring.mail.username}
app.email.from-name=${EMAIL_FROM_NAME:AfterFilm Team}
app.email.reply-to=${EMAIL_REPLY_TO:noreply@afterfilm.com}

# Timeouts y reintentos
app.email.send-timeout=${EMAIL_SEND_TIMEOUT:30000}
app.email.max-retries=${EMAIL_MAX_RETRIES:3}
app.email.retry-delay=${EMAIL_RETRY_DELAY:5000}

# Templates
app.email.templates.confirmation=email/confirmation
app.email.templates.password-reset=email/password-reset
app.email.templates.notification=email/notification

# Debug (desarrollo)
spring.mail.properties.mail.debug=${MAIL_DEBUG:false}
logging.level.org.springframework.mail=${MAIL_LOG_LEVEL:INFO}
logging.level.com.sun.mail=${MAIL_LOG_LEVEL:INFO}
```

### 🤖 Sistema de Moderación con IA

#### Ollama Configuration
```properties
# Configuración básica
app.moderation.enabled=${MODERATION_ENABLED:true}
app.moderation.ollama.url=${OLLAMA_URL:http://localhost:11434}
app.moderation.ollama.model=${OLLAMA_MODEL:llama3}
app.moderation.ollama.timeout=${OLLAMA_TIMEOUT:30}
app.moderation.ollama.enabled=${OLLAMA_ENABLED:true}

# Configuración de umbrales
app.moderation.toxicity.threshold=${TOXICITY_THRESHOLD:0.7}
app.moderation.review.threshold=${REVIEW_THRESHOLD:0.5}
app.moderation.fallback.enabled=${MODERATION_FALLBACK:true}

# Configuración de retry
app.moderation.max-retries=${MODERATION_RETRIES:2}
app.moderation.retry-delay=${MODERATION_RETRY_DELAY:1000}

# Auto-moderación
app.moderation.auto-approve=${AUTO_APPROVE:false}
app.moderation.auto-reject=${AUTO_REJECT:true}
app.moderation.queue-size=${MODERATION_QUEUE:100}
```

#### External AI Services (Alternativo)
```properties
# Configuración para servicios externos de moderación
app.moderation.api.url=${MODERATION_API_URL:http://localhost:5000}
app.moderation.api.key=${MODERATION_API_KEY:your_api_key}
app.moderation.api.timeout=${MODERATION_API_TIMEOUT:15000}

# Configuración de modelos
app.moderation.models.toxicity=${TOXICITY_MODEL:toxic-bert}
app.moderation.models.sentiment=${SENTIMENT_MODEL:sentiment-roberta}
app.moderation.models.spam=${SPAM_MODEL:spam-detector}
```

### 🖼️ Gestión de Imágenes

#### Storage Configuration
```properties
# Configuración básica de almacenamiento
app.images.storage-path=${IMAGES_STORAGE_PATH:./data/images}
app.images.serve-base=${IMAGES_SERVE_BASE:/images}
app.images.max-size=${IMAGE_MAX_SIZE:5242880}
app.images.allowed-types=${ALLOWED_IMAGE_TYPES:image/jpeg,image/png,image/webp}

# Directorios específicos
app.images.posters.path=${app.images.storage-path}/posters
app.images.profiles.path=${app.images.storage-path}/profiles
app.images.backgrounds.path=${app.images.storage-path}/backgrounds

# Configuración de descarga
app.images.download.enabled=${DOWNLOAD_IMAGES:true}
app.images.download.timeout=${DOWNLOAD_TIMEOUT:30000}
app.images.download.max-retries=${DOWNLOAD_RETRIES:3}
app.images.download.user-agent=${DOWNLOAD_USER_AGENT:AfterFilm/1.0}

# Optimización de imágenes
app.images.optimize=${OPTIMIZE_IMAGES:false}
app.images.quality=${IMAGE_QUALITY:85}
app.images.resize.enabled=${RESIZE_IMAGES:false}
app.images.resize.max-width=${MAX_IMAGE_WIDTH:1920}
app.images.resize.max-height=${MAX_IMAGE_HEIGHT:1080}
```

#### CDN Configuration
```properties
# CDN para servir imágenes (opcional)
app.images.cdn.enabled=${CDN_ENABLED:false}
app.images.cdn.base-url=${CDN_BASE_URL:https://cdn.afterfilm.com}
app.images.cdn.provider=${CDN_PROVIDER:cloudinary}

# Configuración Cloudinary (ejemplo)
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:your_cloud}
cloudinary.api-key=${CLOUDINARY_API_KEY:your_key}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:your_secret}
```

### 📈 Cache y Performance

#### Caffeine Cache Configuration
```properties
# Configuración general de cache
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=${CACHE_MAX_SIZE:1000},expireAfterWrite=${CACHE_EXPIRE:30m}

# Configuraciones específicas por cache
app.cache.movies.spec=maximumSize=500,expireAfterWrite=1h
app.cache.users.spec=maximumSize=1000,expireAfterWrite=15m
app.cache.tmdb.spec=maximumSize=200,expireAfterWrite=6h
app.cache.reviews.spec=maximumSize=2000,expireAfterWrite=30m

# Cache de imágenes (metadata)
app.cache.images.spec=maximumSize=5000,expireAfterWrite=24h
app.cache.images.enabled=${IMAGE_CACHE_ENABLED:true}
```

#### Async Configuration
```properties
# Configuración de tareas asíncronas
spring.task.execution.pool.core-size=${ASYNC_CORE_POOL:4}
spring.task.execution.pool.max-size=${ASYNC_MAX_POOL:8}
spring.task.execution.pool.queue-capacity=${ASYNC_QUEUE:100}
spring.task.execution.thread-name-prefix=pelisapp-async-

# Configuración de scheduling
spring.task.scheduling.pool.size=${SCHEDULER_POOL:2}
spring.task.scheduling.thread-name-prefix=pelisapp-scheduler-
```

### 🌐 Server Configuration

#### Embedded Server (Tomcat)
```properties
# Configuración del servidor
server.port=${PORT:8080}
server.address=${SERVER_ADDRESS:0.0.0.0}
server.servlet.context-path=${CONTEXT_PATH:}

# Configuración de conexión
server.tomcat.max-connections=${MAX_CONNECTIONS:8192}
server.tomcat.threads.max=${MAX_THREADS:200}
server.tomcat.threads.min-spare=${MIN_THREADS:10}
server.tomcat.accept-count=${ACCEPT_COUNT:100}
server.tomcat.max-http-form-post-size=${MAX_POST_SIZE:2MB}

# Timeouts
server.tomcat.connection-timeout=${CONNECTION_TIMEOUT:20000}
server.tomcat.keep-alive-timeout=${KEEP_ALIVE_TIMEOUT:20000}
server.tomcat.max-keep-alive-requests=${MAX_KEEP_ALIVE:100}
```

#### SSL/TLS Configuration (Producción)
```properties
# SSL Configuration
server.ssl.enabled=${SSL_ENABLED:false}
server.ssl.key-store=${SSL_KEYSTORE:classpath:keystore.p12}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:password}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=${SSL_KEY_ALIAS:pelisapp}

# SSL Protocols y Ciphers
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
```

### 📊 Monitoring y Observabilidad

#### Actuator Configuration
```properties
# Actuator endpoints
management.endpoints.web.exposure.include=${ACTUATOR_ENDPOINTS:health,info,metrics}
management.endpoint.health.show-details=${HEALTH_SHOW_DETAILS:when-authorized}
management.endpoint.health.show-components=always
management.endpoint.health.roles=ADMIN

# Info endpoint
management.info.env.enabled=true
management.info.java.enabled=true
management.info.os.enabled=true

# Metrics
management.metrics.enabled=true
management.metrics.export.prometheus.enabled=${PROMETHEUS_ENABLED:false}
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.percentiles.http.server.requests=0.5,0.9,0.95,0.99

# Health indicators personalizados
management.health.db.enabled=true
management.health.diskspace.enabled=true
management.health.mail.enabled=${app.email.enabled}
management.health.custom.tmdb.enabled=true
```

#### Logging Configuration
```properties
# Niveles de logging por ambiente
logging.level.root=${LOG_LEVEL_ROOT:INFO}
logging.level.alicanteweb.pelisapp=${LOG_LEVEL_APP:INFO}
logging.level.org.springframework.web=${LOG_LEVEL_WEB:WARN}
logging.level.org.hibernate=${LOG_LEVEL_HIBERNATE:WARN}
logging.level.org.springframework.security=${LOG_LEVEL_SECURITY:WARN}

# Configuración de archivos de log
logging.file.name=${LOG_FILE_NAME:logs/pelisapp.log}
logging.file.max-size=${LOG_FILE_MAX_SIZE:100MB}
logging.file.max-history=${LOG_FILE_MAX_HISTORY:30}
logging.file.total-size-cap=${LOG_FILE_TOTAL_SIZE:3GB}

# Patrón de logging
logging.pattern.console=%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}
```

### 🚦 Rate Limiting

#### Rate Limiting Configuration
```properties
# Rate limiting general
app.rate-limit.enabled=${RATE_LIMIT_ENABLED:true}
app.rate-limit.requests-per-minute=${RATE_LIMIT_RPM:60}
app.rate-limit.requests-per-hour=${RATE_LIMIT_RPH:1000}

# Rate limiting por endpoint
app.rate-limit.api.public=${API_RATE_LIMIT:100}
app.rate-limit.api.admin=${ADMIN_RATE_LIMIT:500}
app.rate-limit.auth.login=${LOGIN_RATE_LIMIT:10}
app.rate-limit.auth.register=${REGISTER_RATE_LIMIT:5}

# Rate limiting para APIs externas
app.rate-limit.tmdb.requests-per-second=${TMDB_RPS:4}
app.rate-limit.email.per-hour=${EMAIL_RATE_LIMIT:10}
```

## 🔧 Perfiles de Configuración

### Profile: Development (dev)
```properties
# application-dev.properties
spring.profiles.active=dev

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/pelisapp_dev
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Logging
logging.level.root=DEBUG
logging.level.alicanteweb.pelisapp=DEBUG

# TMDB
app.tmdb.load-on-startup=false

# Email (mock)
app.email.enabled=false

# Cache (disabled)
spring.cache.type=none

# Seguridad relajada
app.jwt.secret=development-secret-minimum-32-characters-for-HS256
server.ssl.enabled=false
```

### Profile: Testing (test)
```properties
# application-test.properties
spring.profiles.active=test

# Database in-memory
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Logging mínimo
logging.level.root=WARN
logging.level.alicanteweb.pelisapp=INFO

# Deshabilitar integraciones externas
app.tmdb.load-on-startup=false
app.email.enabled=false
app.moderation.enabled=false

# Cache simple
spring.cache.type=simple
```

### Profile: Production (prod)
```properties
# application-prod.properties
spring.profiles.active=prod

# Database (configurado via env vars)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Logging optimizado
logging.level.root=WARN
logging.level.alicanteweb.pelisapp=INFO

# Seguridad reforzada
server.ssl.enabled=true
app.cors.allowed-origins=${PROD_ALLOWED_ORIGINS}

# Cache completo
spring.cache.type=caffeine

# Todas las integraciones habilitadas
app.email.enabled=true
app.moderation.enabled=true
app.tmdb.load-on-startup=true
```

## 🔐 Gestión de Secretos

### Variables de Entorno Críticas
```bash
# OBLIGATORIO cambiar en producción
export APP_JWT_SECRET="$(openssl rand -base64 32)"
export SPRING_DATASOURCE_PASSWORD="secure_database_password"
export SPRING_MAIL_PASSWORD="gmail_app_password"

# API Keys
export TMDB_API_KEY="your_tmdb_api_key"
export TMDB_BEARER_TOKEN="your_tmdb_bearer_token"

# SSL
export SSL_KEYSTORE_PASSWORD="secure_keystore_password"

# URLs
export APP_BASE_URL="https://your-domain.com"
export DATABASE_URL="jdbc:mysql://prod-db:3306/pelisapp"
```

### Docker Secrets (Docker Swarm)
```yaml
# docker-compose.secrets.yml
version: '3.8'

services:
  app:
    image: pelisapp:latest
    secrets:
      - jwt_secret
      - db_password
      - tmdb_bearer_token
      - ssl_keystore_password
    environment:
      - APP_JWT_SECRET_FILE=/run/secrets/jwt_secret
      - SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password

secrets:
  jwt_secret:
    external: true
  db_password:
    external: true
  tmdb_bearer_token:
    external: true
  ssl_keystore_password:
    external: true
```

### Kubernetes Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: pelisapp-secrets
type: Opaque
stringData:
  jwt-secret: "your-base64-encoded-secret"
  database-password: "secure-db-password"
  tmdb-bearer-token: "bearer-token"
  mail-password: "gmail-app-password"
```

## 📋 Configuración por Función

### 🎬 Solo API (Sin Web UI)
```properties
# Configuración para API-only
spring.web.resources.static-locations=
spring.thymeleaf.enabled=false
spring.mvc.static-path-pattern=/api/**
server.servlet.context-path=/api
```

### 📱 Solo Web (Sin API)
```properties
# Configuración para Web-only  
management.endpoints.web.exposure.include=health
app.api.enabled=false
security.api.disabled=true
```

### 🔧 Microservicio
```properties
# Configuración para microservicio
spring.application.name=pelisapp-service
spring.cloud.config.enabled=true
eureka.client.register-with-eureka=true
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

---

Esta configuración está diseñada para ser **flexible**, **segura** y **escalable**, permitiendo adaptar AfterFilm a cualquier entorno y necesidad específica. 🔧
