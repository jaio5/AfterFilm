# 🚀 Guía de Deployment - AfterFilm

Esta guía cubre el despliegue de AfterFilm en diferentes entornos, desde desarrollo local hasta producción en la nube.

## 🎯 Estrategias de Deployment

### Entornos Soportados
- **Desarrollo Local** - Para testing y desarrollo
- **Staging/Testing** - Entorno de pruebas pre-producción  
- **Producción** - Entorno de producción escalable
- **Docker** - Containerización para cualquier entorno
- **Cloud Platforms** - AWS, Google Cloud, Azure, Heroku

## 🐳 Deployment con Docker

### Dockerfile Optimizado

```dockerfile
# Multi-stage build para optimizar tamaño
FROM maven:3.9.4-eclipse-temurin-17-alpine AS builder

# Crear directorio de trabajo
WORKDIR /app

# Copiar solo archivos necesarios para dependencias (cache layer)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src src

# Compilar aplicación (skip tests en build para rapidez)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Crear directorios necesarios
WORKDIR /app
RUN mkdir -p data/images && \
    chown -R appuser:appgroup /app

# Copiar JAR desde build stage
COPY --from=builder /app/target/AfterFilm-*.jar app.jar

# Cambiar a usuario no-root
USER appuser

# Configurar JVM para containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Exponer puerto
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose - Desarrollo

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_HOST=db
      - SPRING_DATASOURCE_PORT=3306
      - SPRING_DATASOURCE_DB=pelisapp
      - SPRING_DATASOURCE_USERNAME=pelisapp
      - SPRING_DATASOURCE_PASSWORD=pelisapp123
      - APP_JWT_SECRET=development-secret-key-change-in-production-32-chars
      - TMDB_API_KEY=${TMDB_API_KEY}
      - TMDB_BEARER_TOKEN=${TMDB_BEARER_TOKEN}
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    restart: unless-stopped
    networks:
      - pelisapp-network

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
      - ./scripts/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 10s
      retries: 5
      interval: 30s
    restart: unless-stopped
    networks:
      - pelisapp-network

  # Opcional: Ollama para moderación IA
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped
    networks:
      - pelisapp-network

  # Opcional: Reverse proxy
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - app
    restart: unless-stopped
    networks:
      - pelisapp-network

volumes:
  db_data:
  ollama_data:

networks:
  pelisapp-network:
    driver: bridge
```

### Docker Compose - Producción

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  app:
    image: pelisapp:latest
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=${DATABASE_URL}
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - APP_JWT_SECRET=${JWT_SECRET}
      - TMDB_API_KEY=${TMDB_API_KEY}
      - TMDB_BEARER_TOKEN=${TMDB_BEARER_TOKEN}
      - SPRING_MAIL_USERNAME=${MAIL_USERNAME}
      - SPRING_MAIL_PASSWORD=${MAIL_PASSWORD}
    volumes:
      - app_data:/app/data
      - app_logs:/app/logs
    networks:
      - pelisapp-prod
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  db:
    image: mysql:8.0
    environment:
      - MYSQL_DATABASE=${DB_NAME}
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
    volumes:
      - db_data:/var/lib/mysql
      - ./backups:/backups
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
    networks:
      - pelisapp-prod
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --innodb-buffer-pool-size=1G
      --max-connections=200

volumes:
  db_data:
  app_data:
  app_logs:

networks:
  pelisapp-prod:
    driver: overlay
    external: true
```

## ☁️ Cloud Deployment

### AWS - Elastic Container Service (ECS)

#### Task Definition
```json
{
  "family": "pelisapp",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::account:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "pelisapp",
      "image": "your-account.dkr.ecr.region.amazonaws.com/pelisapp:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:ssm:region:account:parameter/pelisapp/db-url"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:pelisapp/jwt-secret"
        }
      ],
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/pelisapp",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

#### CloudFormation Template (Excerpt)
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: 'AfterFilm Infrastructure'

Parameters:
  Environment:
    Type: String
    Default: prod
    AllowedValues: [dev, staging, prod]

Resources:
  # VPC and Networking
  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: 10.0.0.0/16
      EnableDnsHostnames: true
      EnableDnsSupport: true
      Tags:
        - Key: Name
          Value: !Sub "${Environment}-pelisapp-vpc"

  # RDS Database
  DatabaseSubnetGroup:
    Type: AWS::RDS::DBSubnetGroup
    Properties:
      DBSubnetGroupDescription: Subnet group for AfterFilm database
      SubnetIds:
        - !Ref PrivateSubnet1
        - !Ref PrivateSubnet2
      Tags:
        - Key: Name
          Value: !Sub "${Environment}-pelisapp-db-subnet-group"

  Database:
    Type: AWS::RDS::DBInstance
    Properties:
      DBInstanceIdentifier: !Sub "${Environment}-pelisapp-db"
      DBInstanceClass: db.t3.micro
      Engine: MySQL
      EngineVersion: '8.0'
      AllocatedStorage: 20
      StorageType: gp2
      DatabaseName: pelisapp
      MasterUsername: !Ref DatabaseUsername
      MasterUserPassword: !Ref DatabasePassword
      DBSubnetGroupName: !Ref DatabaseSubnetGroup
      VPCSecurityGroups:
        - !Ref DatabaseSecurityGroup
      BackupRetentionPeriod: 7
      MultiAZ: false
      PubliclyAccessible: false

  # ECS Cluster
  ECSCluster:
    Type: AWS::ECS::Cluster
    Properties:
      ClusterName: !Sub "${Environment}-pelisapp-cluster"
      CapacityProviders:
        - FARGATE
        - FARGATE_SPOT
      DefaultCapacityProviderStrategy:
        - CapacityProvider: FARGATE
          Weight: 1

  # Application Load Balancer
  LoadBalancer:
    Type: AWS::ElasticLoadBalancingV2::LoadBalancer
    Properties:
      Name: !Sub "${Environment}-pelisapp-alb"
      Scheme: internet-facing
      Type: application
      Subnets:
        - !Ref PublicSubnet1
        - !Ref PublicSubnet2
      SecurityGroups:
        - !Ref LoadBalancerSecurityGroup
```

### Google Cloud Platform - Cloud Run

#### deploy.yaml
```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: pelisapp
  annotations:
    run.googleapis.com/ingress: all
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/minScale: "1"
        autoscaling.knative.dev/maxScale: "10"
        run.googleapis.com/cpu-throttling: "false"
        run.googleapis.com/execution-environment: gen2
    spec:
      serviceAccountName: pelisapp-sa@project-id.iam.gserviceaccount.com
      containers:
      - image: gcr.io/project-id/pelisapp:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: pelisapp-secrets
              key: database-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: pelisapp-secrets
              key: jwt-secret
        resources:
          limits:
            cpu: 1000m
            memory: 2Gi
          requests:
            cpu: 500m
            memory: 1Gi
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
        readinessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Heroku Deployment

#### Procfile
```
web: java -Dserver.port=$PORT $JAVA_OPTS -jar target/AfterFilm-*.jar
```

#### heroku.yml
```yaml
build:
  docker:
    web: Dockerfile
run:
  web: java -Dserver.port=$PORT $JAVA_OPTS -jar /app/app.jar
```

#### Scripts de Deployment
```bash
#!/bin/bash
# deploy-heroku.sh

# Configurar variables de entorno
heroku config:set SPRING_PROFILES_ACTIVE=prod -a your-app-name
heroku config:set APP_JWT_SECRET="$(openssl rand -base64 32)" -a your-app-name
heroku config:set TMDB_API_KEY="your-tmdb-key" -a your-app-name

# Configurar add-ons
heroku addons:create cleardb:ignite -a your-app-name  # MySQL
heroku addons:create papertrail:choklad -a your-app-name  # Logging
heroku addons:create scheduler:standard -a your-app-name  # Cron jobs

# Deploy
git push heroku main

# Ejecutar migraciones si es necesario
heroku run java -cp target/classes:target/lib/* org.springframework.boot.loader.JarLauncher --spring.profiles.active=prod
```

## 🔧 Configuración por Entorno

### Variables de Entorno por Ambiente

#### Desarrollo (.env.dev)
```bash
# Base de datos
SPRING_DATASOURCE_HOST=localhost
SPRING_DATASOURCE_PORT=3306
SPRING_DATASOURCE_DB=pelisapp_dev
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=password

# JWT (desarrollo - puede ser simple)
APP_JWT_SECRET=development-secret-minimum-32-characters

# TMDB
TMDB_API_KEY=your-dev-key
TMDB_BEARER_TOKEN=your-dev-token

# Email (mock en desarrollo)
SPRING_MAIL_USERNAME=dev@example.com
SPRING_MAIL_PASSWORD=mock-password
APP_EMAIL_ENABLED=false

# Configuraciones de desarrollo
APP_DEV_MODE=true
LOGGING_LEVEL_ROOT=DEBUG
SPRING_JPA_SHOW_SQL=true
```

#### Staging (.env.staging)
```bash
# Base de datos staging
DATABASE_URL=jdbc:mysql://staging-db:3306/pelisapp_staging
DB_USERNAME=pelisapp_staging
DB_PASSWORD=${STAGING_DB_PASSWORD}

# JWT
JWT_SECRET=${STAGING_JWT_SECRET}

# APIs externas
TMDB_API_KEY=${TMDB_API_KEY}
TMDB_BEARER_TOKEN=${TMDB_BEARER_TOKEN}

# Email real pero limitado
SPRING_MAIL_USERNAME=${STAGING_MAIL_USERNAME}
SPRING_MAIL_PASSWORD=${STAGING_MAIL_PASSWORD}
APP_EMAIL_ENABLED=true

# Configuración staging
APP_DEV_MODE=false
LOGGING_LEVEL_ROOT=INFO
SPRING_JPA_SHOW_SQL=false
```

#### Producción (.env.prod)
```bash
# Base de datos producción
DATABASE_URL=${PROD_DATABASE_URL}
DB_USERNAME=${PROD_DB_USERNAME}  
DB_PASSWORD=${PROD_DB_PASSWORD}

# JWT (OBLIGATORIO - secret fuerte)
JWT_SECRET=${PROD_JWT_SECRET}

# APIs externas
TMDB_API_KEY=${PROD_TMDB_API_KEY}
TMDB_BEARER_TOKEN=${PROD_TMDB_BEARER_TOKEN}

# Email producción
SPRING_MAIL_USERNAME=${PROD_MAIL_USERNAME}
SPRING_MAIL_PASSWORD=${PROD_MAIL_PASSWORD}
APP_EMAIL_ENABLED=true

# Base URL para emails
APP_BASE_URL=https://your-domain.com

# Configuración producción
APP_DEV_MODE=false
LOGGING_LEVEL_ROOT=WARN
SPRING_JPA_SHOW_SQL=false

# Seguridad adicional
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=${SSL_KEYSTORE_PATH}
SERVER_SSL_KEY_STORE_PASSWORD=${SSL_KEYSTORE_PASSWORD}
```

## 🚦 CI/CD Pipeline

### GitHub Actions

#### .github/workflows/ci-cd.yml
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: pelisapp_test
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'eclipse-temurin'

    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2

    - name: Run tests
      run: mvn clean test
      env:
        SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/pelisapp_test
        SPRING_DATASOURCE_USERNAME: root
        SPRING_DATASOURCE_PASSWORD: root

    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    permissions:
      contents: read
      packages: write

    steps:
    - uses: actions/checkout@v4

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3

    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix=commit-
          type=raw,value=latest,enable={{is_default_branch}}

    - name: Build and push Docker image
      uses: docker/build-push-action@v5
      with:
        context: .
        platforms: linux/amd64,linux/arm64
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'

    steps:
    - uses: actions/checkout@v4

    - name: Deploy to production
      run: |
        echo "Deploying to production..."
        # Aquí irían los comandos específicos del proveedor cloud
        # Ejemplo para AWS ECS:
        # aws ecs update-service --cluster prod-cluster --service pelisapp-service --force-new-deployment
```

### GitLab CI/CD

#### .gitlab-ci.yml
```yaml
stages:
  - test
  - build
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  MAVEN_CLI_OPTS: "--batch-mode --errors --fail-at-end --show-version"

cache:
  paths:
    - .m2/repository/
    - target/

test:
  stage: test
  image: maven:3.9.4-eclipse-temurin-17
  services:
    - name: mysql:8.0
      alias: mysql
      variables:
        MYSQL_ROOT_PASSWORD: root
        MYSQL_DATABASE: pelisapp_test
  script:
    - mvn $MAVEN_CLI_OPTS test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
    paths:
      - target/
  coverage: '/Total.*?([0-9]{1,3})%/'

build:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:latest
    - docker push $CI_REGISTRY_IMAGE:latest
  only:
    - main

deploy:production:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache curl
  script:
    - echo "Deploying to production..."
    # Deployment commands specific to your infrastructure
  environment:
    name: production
    url: https://your-app.com
  only:
    - main
  when: manual
```

## 📊 Monitoring y Logging

### Configuración de Logging

#### logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="!prod">
        <!-- Desarrollo: logs coloridos en consola -->
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="alicanteweb.pelisapp" level="DEBUG"/>
    </springProfile>

    <springProfile name="prod">
        <!-- Producción: logs estructurados -->
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/app/logs/pelisapp.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>/app/logs/pelisapp-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                </providers>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="CONSOLE"/>
        </root>
        
        <logger name="alicanteweb.pelisapp" level="INFO"/>
        <logger name="org.springframework.web" level="WARN"/>
        <logger name="org.hibernate" level="WARN"/>
    </springProfile>
</configuration>
```

### Health Checks y Metrics

#### application-prod.properties
```properties
# Actuator endpoints para monitoring
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.endpoint.health.roles=ADMIN
management.metrics.export.prometheus.enabled=true

# Health checks personalizados
management.health.db.enabled=true
management.health.diskspace.enabled=true
management.health.mail.enabled=true

# Application info
info.app.name=AfterFilm
info.app.description=Movie Review Platform
info.app.version=@project.version@
info.app.encoding=@project.build.sourceEncoding@
info.app.java.version=@java.version@
```

## 🔒 Seguridad en Producción

### Configuraciones de Seguridad

#### application-prod.properties (Security)
```properties
# SSL/TLS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.keyStoreType=PKCS12
server.ssl.keyAlias=pelisapp

# Security headers
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict

# CORS restrictivo
app.cors.allowed-origins=https://your-domain.com,https://www.your-domain.com
app.cors.allowed-methods=GET,POST,PUT,DELETE
app.cors.allowed-headers=Content-Type,Authorization
app.cors.max-age=3600

# Rate limiting
app.rate-limit.enabled=true
app.rate-limit.requests-per-minute=100
```

### Nginx Configuration

#### nginx/nginx.conf
```nginx
events {
    worker_connections 1024;
}

http {
    upstream pelisapp {
        server app:8080;
    }

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

    server {
        listen 80;
        server_name your-domain.com www.your-domain.com;
        
        # Redirect to HTTPS
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name your-domain.com www.your-domain.com;

        # SSL Configuration
        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;

        # Security headers
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";

        # Static files
        location /images/ {
            alias /app/data/images/;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # API endpoints
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://pelisapp;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Main application
        location / {
            proxy_pass http://pelisapp;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

## 🔄 Backup y Recovery

### Script de Backup Automático

#### scripts/backup-database.sh
```bash
#!/bin/bash

# Configuración
DB_HOST=${DB_HOST:-localhost}
DB_USER=${DB_USER:-pelisapp}
DB_PASSWORD=${DB_PASSWORD}
DB_NAME=${DB_NAME:-pelisapp}
BACKUP_DIR=${BACKUP_DIR:-/backups}
RETENTION_DAYS=${RETENTION_DAYS:-7}

# Crear directorio de backups
mkdir -p $BACKUP_DIR

# Generar nombre de archivo con timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/pelisapp_backup_$TIMESTAMP.sql"

echo "🚀 Iniciando backup de base de datos..."

# Crear backup
mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASSWORD \
    --single-transaction \
    --routines \
    --triggers \
    $DB_NAME > $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "✅ Backup completado: $BACKUP_FILE"
    
    # Comprimir backup
    gzip $BACKUP_FILE
    echo "📦 Backup comprimido: $BACKUP_FILE.gz"
    
    # Limpiar backups antiguos
    find $BACKUP_DIR -name "pelisapp_backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete
    echo "🧹 Backups antiguos eliminados (>$RETENTION_DAYS días)"
    
else
    echo "❌ Error en el backup"
    exit 1
fi
```

### Recovery Script

#### scripts/restore-database.sh
```bash
#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Uso: $0 <archivo_backup>"
    echo "Ejemplo: $0 /backups/pelisapp_backup_20240115_143000.sql.gz"
    exit 1
fi

BACKUP_FILE=$1
DB_HOST=${DB_HOST:-localhost}
DB_USER=${DB_USER:-pelisapp}
DB_PASSWORD=${DB_PASSWORD}
DB_NAME=${DB_NAME:-pelisapp}

echo "⚠️  ADVERTENCIA: Este script restaurará la base de datos desde $BACKUP_FILE"
echo "   Todos los datos actuales se perderán."
read -p "¿Continuar? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operación cancelada."
    exit 1
fi

echo "🚀 Iniciando restauración..."

# Descomprimir si es necesario
if [[ $BACKUP_FILE == *.gz ]]; then
    echo "📦 Descomprimiendo backup..."
    TEMP_FILE="/tmp/pelisapp_restore_$(date +%s).sql"
    gunzip -c $BACKUP_FILE > $TEMP_FILE
    BACKUP_FILE=$TEMP_FILE
fi

# Restaurar base de datos
mysql -h $DB_HOST -u $DB_USER -p$DB_PASSWORD $DB_NAME < $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "✅ Restauración completada exitosamente"
    
    # Limpiar archivo temporal si se creó
    if [ ! -z "$TEMP_FILE" ]; then
        rm -f $TEMP_FILE
    fi
else
    echo "❌ Error en la restauración"
    exit 1
fi
```

## 📋 Checklist de Deployment

### Pre-Deployment

- [ ] ✅ Todas las pruebas unitarias e integración pasan
- [ ] ✅ Código revisado y aprobado
- [ ] ✅ Variables de entorno configuradas
- [ ] ✅ Secretos y credenciales seguros configurados
- [ ] ✅ Base de datos migrada y testeada
- [ ] ✅ SSL/TLS certificados configurados
- [ ] ✅ Backup de base de datos actual creado
- [ ] ✅ Plan de rollback preparado
- [ ] ✅ Monitoring y alertas configurados
- [ ] ✅ Load balancer y health checks configurados

### Post-Deployment

- [ ] ✅ Aplicación responde en todos los endpoints críticos
- [ ] ✅ Health checks funcionando
- [ ] ✅ Logs generándose correctamente
- [ ] ✅ Base de datos accesible y funcional
- [ ] ✅ Integración TMDB funcionando
- [ ] ✅ Sistema de email operativo
- [ ] ✅ Moderación IA funcionando (si aplicable)
- [ ] ✅ Performance dentro de parámetros esperados
- [ ] ✅ Usuarios pueden registrarse y loguearse
- [ ] ✅ Funcionalidades críticas verificadas

---

Esta guía de deployment está diseñada para garantizar despliegues **seguros**, **confiables** y **escalables** de AfterFilm en cualquier entorno de producción. 🚀
