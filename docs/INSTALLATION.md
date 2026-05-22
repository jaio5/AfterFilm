# 📦 Guía de Instalación - AfterFilm

Esta guía te llevará paso a paso a través del proceso de instalación y configuración de AfterFilm en tu entorno de desarrollo.

## 🔧 Prerrequisitos

### Software Requerido
- **Java 17+** ([OpenJDK](https://adoptium.net/) o Oracle JDK)
- **Maven 3.9+** ([Descargar](https://maven.apache.org/download.cgi))
- **MySQL 8.0+** ([Descargar](https://dev.mysql.com/downloads/mysql/))
- **Git** ([Descargar](https://git-scm.com/))

### Software Opcional
- **Ollama** ([Instalar](https://ollama.ai/)) - Para moderación con IA
- **Postman** o **Thunder Client** - Para testing de API

### Hardware Recomendado
- **RAM**: 4GB mínimo, 8GB recomendado
- **CPU**: Dual-core mínimo
- **Disco**: 2GB de espacio libre

## 📥 Instalación

### 1. Clonar el Repositorio
```bash
git clone [url-del-repositorio]
cd AfterFilm
```

### 2. Configurar Base de Datos MySQL

#### Crear Base de Datos
```sql
-- Conectarse a MySQL como root
mysql -u root -p

-- Crear base de datos
CREATE DATABASE pelisapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Crear usuario específico (opcional pero recomendado)
CREATE USER 'pelisapp_user'@'localhost' IDENTIFIED BY 'tu_password_seguro';
GRANT ALL PRIVILEGES ON pelisapp.* TO 'pelisapp_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### Verificar Conexión
```bash
mysql -u pelisapp_user -p pelisapp
```

### 3. Configurar Variables de Entorno

#### Opción A: Variables del Sistema
```bash
# Windows (PowerShell)
$env:SPRING_DATASOURCE_USERNAME="pelisapp_user"
$env:SPRING_DATASOURCE_PASSWORD="tu_password_seguro"
$env:SPRING_DATASOURCE_DB="pelisapp"
$env:APP_JWT_SECRET="tu_jwt_secret_minimo_32_caracteres_para_HS256"
$env:TMDB_API_KEY="tu_tmdb_api_key"
$env:TMDB_BEARER_TOKEN="tu_tmdb_bearer_token"

# Linux/macOS
export SPRING_DATASOURCE_USERNAME="pelisapp_user"
export SPRING_DATASOURCE_PASSWORD="tu_password_seguro"
export SPRING_DATASOURCE_DB="pelisapp"
export APP_JWT_SECRET="tu_jwt_secret_minimo_32_caracteres_para_HS256"
export TMDB_API_KEY="tu_tmdb_api_key"
export TMDB_BEARER_TOKEN="tu_tmdb_bearer_token"
```

#### Opción B: Archivo .env (Crear en raíz del proyecto)
```env
# Base de datos
SPRING_DATASOURCE_HOST=localhost
SPRING_DATASOURCE_PORT=3306
SPRING_DATASOURCE_DB=pelisapp
SPRING_DATASOURCE_USERNAME=pelisapp_user
SPRING_DATASOURCE_PASSWORD=tu_password_seguro

# JWT Configuración
APP_JWT_SECRET=tu_jwt_secret_minimo_32_caracteres_para_HS256_security
APP_JWT_ACCESS_EXPIRATION_MS=86400000
APP_JWT_REFRESH_EXPIRATION_MS=604800000

# TMDB API
TMDB_API_KEY=tu_tmdb_api_key_desde_themoviedb_org
TMDB_BEARER_TOKEN=tu_bearer_token_desde_tmdb_dashboard

# Email (opcional para confirmación de registro)
SPRING_MAIL_USERNAME=tu_email@gmail.com
SPRING_MAIL_PASSWORD=tu_app_password_de_gmail

# URL base (para emails de confirmación)
APP_BASE_URL=http://localhost:8080
```

### 4. Obtener Credenciales TMDB

1. **Registrarse** en [The Movie Database](https://www.themoviedb.org/)
2. **Ir a Settings** → **API**
3. **Crear API Key** (v3 auth)
4. **Crear Access Token** (v4 auth) - Recomendado
5. **Copiar** tanto API Key como Bearer Token

### 5. Configurar Email (Opcional)

#### Para Gmail:
1. **Activar autenticación de 2 factores** en tu cuenta Google
2. **Generar App Password**:
   - Google Account → Security → App passwords
   - Seleccionar "Mail" y tu dispositivo
   - Usar la contraseña generada en `SPRING_MAIL_PASSWORD`

### 6. Instalar y Configurar Ollama (Opcional)

#### Instalación
```bash
# Instalar Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Descargar modelo para moderación (recomendado: llama3)
ollama pull llama3

# Verificar instalación
ollama list
```

#### Configuración para AfterFilm
```properties
# En application.properties (ya configurado por defecto)
app.moderation.ollama.url=http://localhost:11434
app.moderation.ollama.model=llama3
app.moderation.ollama.enabled=true
```

### 7. Compilar e Instalar Dependencias

```bash
# Limpiar y compilar proyecto
mvn clean compile

# Ejecutar tests (opcional)
mvn test

# Instalar dependencias
mvn clean install
```

### 8. Ejecutar la Aplicación

```bash
# Ejecutar con Maven
mvn spring-boot:run

# O ejecutar el JAR directamente
java -jar target/AfterFilm-0.0.1-SNAPSHOT.jar
```

## ✅ Verificación de Instalación

### 1. Verificar Servidor
- **URL**: http://localhost:8080
- **Estado**: Página principal debe cargar correctamente

### 2. Verificar API
```bash
# Health check
curl http://localhost:8080/api/health

# Respuesta esperada:
# {"status":"UP","timestamp":"2024-..."}
```

### 3. Verificar Base de Datos
```bash
# Verificar que las tablas se crearon automáticamente
mysql -u pelisapp_user -p pelisapp -e "SHOW TABLES;"
```

### 4. Verificar TMDB Integration
```bash
# Test TMDB connection (requiere estar logueado como ADMIN)
curl -X GET http://localhost:8080/api/admin/tmdb/test \
  -H "Authorization: Bearer tu_jwt_token"
```

## 🔧 Configuración Adicional

### Perfiles de Spring

#### Desarrollo (por defecto)
```properties
# application-dev.properties
spring.jpa.show-sql=true
app.dev-mode=true
logging.level.alicanteweb.pelisapp=DEBUG
```

#### Producción
```properties
# application-prod.properties
spring.jpa.show-sql=false
app.dev-mode=false
logging.level.root=WARN
server.port=80
```

### Configuración de Imágenes
```properties
# Directorio para almacenar posters descargados
app.images.storage-path=./data/images
app.images.serve-base=/images

# Crear directorio si no existe
mkdir -p data/images/posters
mkdir -p data/images/profiles
```

### Configuración de Cache
```properties
# Caffeine cache (ya configurado)
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=30m
```

## 🚨 Solución de Problemas Comunes

### Error: "Access denied for user"
```bash
# Verificar credenciales de MySQL
mysql -u pelisapp_user -p

# Si no funciona, resetear password
ALTER USER 'pelisapp_user'@'localhost' IDENTIFIED BY 'nuevo_password';
FLUSH PRIVILEGES;
```

### Error: "Unable to connect to TMDB"
1. **Verificar** API key y Bearer token
2. **Comprobar** conectividad a internet
3. **Validar** configuración en application.properties

### Error: "JWT Secret too short"
- **El secreto JWT** debe tener al menos 32 caracteres
- **Generar uno seguro**: `openssl rand -base64 32`

### Error: "Cannot send email"
1. **Verificar** App Password de Gmail (no la contraseña normal)
2. **Comprobar** configuración SMTP
3. **Temporal**: Deshabilitar email con `app.email.enabled=false`

### Error: "Ollama connection failed"
```bash
# Verificar que Ollama está ejecutándose
ollama list

# Reiniciar Ollama
ollama serve

# Verificar puerto
curl http://localhost:11434/api/tags
```

### Puerto 8080 en uso
```bash
# Cambiar puerto en application.properties
server.port=8081

# O matar proceso que usa el puerto
# Windows
netstat -ano | findstr :8080
taskkill /PID [PID] /F

# Linux/macOS
lsof -ti :8080 | xargs kill
```

## 📋 Lista de Verificación Pre-Producción

- [ ] ✅ Java 17+ instalado y configurado
- [ ] ✅ Maven 3.9+ instalado
- [ ] ✅ MySQL 8.0+ instalado y ejecutándose
- [ ] ✅ Base de datos `pelisapp` creada
- [ ] ✅ Usuario MySQL configurado con permisos
- [ ] ✅ Variables de entorno configuradas
- [ ] ✅ TMDB API credentials obtenidas y configuradas
- [ ] ✅ JWT secret configurado (mínimo 32 chars)
- [ ] ✅ Aplicación compila sin errores
- [ ] ✅ Tests pasan exitosamente
- [ ] ✅ Aplicación inicia en puerto 8080
- [ ] ✅ Health check responde correctamente
- [ ] ✅ Página principal carga correctamente
- [ ] ✅ (Opcional) Ollama instalado y modelo descargado
- [ ] ✅ (Opcional) Email SMTP configurado y funcionando

## 🎯 Próximos Pasos

Una vez completada la instalación:

1. **[Revisar la Arquitectura](ARCHITECTURE.md)** para entender la estructura
2. **[Consultar la API](API.md)** para integrar con frontend o terceros
3. **[Seguir la Guía de Desarrollador](DEVELOPER.md)** para contribuir al proyecto
4. **[Configurar Deployment](DEPLOYMENT.md)** para producción

---

💡 **Tip**: Guarda las credenciales y configuraciones en un gestor de contraseñas seguro, especialmente para entornos de producción.
