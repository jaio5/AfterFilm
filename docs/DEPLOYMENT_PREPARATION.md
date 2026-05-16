# Plan de Preparación para Despliegue en Producción

Este documento resume los pasos recomendados para preparar PelisApp para un despliegue real, escalable y mantenible. Incluye: arquitectura sugerida, separación de recursos (imágenes), prácticas de escalado, CI/CD, monitorización, seguridad y consideraciones para una futura migración del frontend a React/Next.js.

## Objetivos

- Desplegar una versión estable y segura en producción.
- Escalar horizontalmente para soportar altas tasas de peticiones.
- Quitar la carga de servir imágenes del backend (almacenamiento externo + CDN).
- Asegurar observabilidad, backups, y despliegues reproducibles.
- Preparar el camino para migrar el frontend a React/Next.js en el futuro.

## Resumen de la arquitectura recomendada

- Contenerizar la aplicación Java (Spring Boot) con Docker.
- Desplegar en Kubernetes (EKS/GKE/AKS) o usar servicios gestionados (ECS, App Runner) según las preferencias.
- Base de datos relacional gestionada: Amazon RDS / Cloud SQL (MySQL) o equivalente.
- Almacenamiento de imágenes y assets: Object Storage (S3, GCS, Azure Blob) + CDN (CloudFront/Cloud CDN).
- Cache y sesiones: Redis (ElastiCache o Memorystore). Sessions stateless en la app o externalizadas en Redis.
- Message broker para tareas largas/colas: RabbitMQ o Amazon SQS (con workers dedicados).
- Observabilidad: Prometheus + Grafana para métricas; Loki/ELK para logs; tracer (OpenTelemetry).

## Pasos concretos (ordenados)

1. Contenerizar la app
   - Añadir `Dockerfile` si no existe (optimizado, multi-stage, JRE slim).
   - Construir imagen y probar localmente.
   - Ejemplo de build:

```bash
docker build -t pelisapp:latest .
docker run --rm -e SPRING_PROFILES_ACTIVE=prod -p 8080:8080 pelisapp:latest
```

2. Configuración de entorno y secrets
   - Externalizar configuración por variables de entorno y usar un gestor de secretos (AWS Secrets Manager, HashiCorp Vault, Azure KeyVault).
   - No almacenar credenciales en `application.properties` en el repositorio.

3. Persistencia y migraciones
   - Usar una base de datos gestionada (RDS/Cloud SQL). Habilitar backups automáticos y snapshots.
   - Añadir migraciones con Flyway o Liquibase para versionado del esquema.

4. Separar imágenes/media (migration + runtime)
   - Implementar un servicio `ImageStorage` que use Object Storage (S3/GCS). Mantener interfaz para poder mockear en tests.
   - La app ya soporta `app.images.storage.provider=local|s3`; con `s3` las imágenes se publican mediante URL externa/CDN y no dependen del disco local.
   - Pasos para migrar imágenes existentes:
     - Exportar `data/images/*` a un bucket (preservar estructura y nombres).
     - Actualizar referencias en la BD (si hay rutas absolutas) para apuntar a URLs del bucket.
     - Configurar políticas de bucket (readonly público o URLs firmadas según requisitos).
     - Activar un CDN delante del bucket para servir assets rápidamente y reducir latencia.

5. Hacer la app stateless
   - Evitar escribir estado en disco local. Si se necesita storage temporal, usar Volumes o un sistema de ficheros compartido (EFS) con cuidado.
   - Externalizar sesiones en Redis o usar JWT.

6. Escalado y alta disponibilidad
   - Habilitar readiness/liveness probes en Kubernetes.
   - Configurar HPA (Horizontal Pod Autoscaler) basado en CPU y latencia/throughput (o metrics personalizados).
   - Usar al menos 2 réplicas por servicio y 3 zonas de disponibilidad para la BD gestionada.

7. Caching y rate-limiting
   - Cachear respuestas pesadas (páginas, listados, resultados TMDB) en Redis.
   - Usar TTL razonables y cache-invalidation cuando se actualicen recursos.
   - Aplicar rate limiting (API Gateway, ingress controller o filtro en la app) para proteger endpoints.

8. Colas y tareas asíncronas
   - Mover tareas largas (descarga de posters, procesamiento de imágenes) a workers desacoplados.
   - Usar RabbitMQ/SQS o Spring Cloud Stream para comunicación y scaling independiente.

9. Observabilidad y logs
   - Instrumentar métricas con Micrometer -> Prometheus.
   - Centralizar logs (Loki/ELK/Cloud provider logs) y capturar errores con Sentry (opcional).
   - Configurar alertas en base a errores, latencia, saturación de CPU/memoria.

10. CI/CD y despliegue
    - Crear pipeline (GitHub Actions / GitLab CI) que realice:
      - `mvn -DskipTests package` y construcción de la imagen Docker.
      - Escáner de seguridad (Dependabot/Snyk/OWASP Dependency-Check).
      - Pruebas unitarias y básicas de integración.
      - Deploy a staging (helm/kubectl) y promoción a prod con aprobación manual.

11. Pruebas de carga y tuning
    - Usar k6 o JMeter para simular tráfico y encontrar cuellos de botella.
    - Medir RPS, latencia p95/p99, tiempo de GC, uso de threads.

12. Backups y rollback
    - Backups automáticos de la BD y pruebas periódicas de restauración.
    - Mantener imágenes de despliegue versionadas y scripts de rollback.

13. Seguridad
    - TLS obligatorio (terminación en Load Balancer/Ingress).
    - Revisar dependencias y aplicar políticas de seguridad.
    - Uso de CSP, saneamiento de inputs y limitación de upload sizes.
    - Evitar exponer endpoints administrativos públicamente.

14. Consideraciones de coste
    - Usar tiers gestionados al inicio y optimizar recursos según métricas.
    - Usar CDN + object storage para reducir costos de tráfico del backend.

15. Preparación para migración frontend (React/Next.js)
    - Diseñar API REST/GraphQL estable y versionada.
    - Separar autenticación (JWT/OAuth) para facilitar integración con SPA.
    - Servir assets estáticos desde CDN; el frontend puede ser desplegado en Vercel/Netlify o en el mismo CDN.

## Cambios de código recomendados (alta prioridad)

- Añadir una interfaz `ImageStorage` y una implementación `S3ImageStorage` que reemplace el almacenamiento en disco.
- Asegurar que todas las rutas que devuelven imágenes devuelven URLs externas (no proxear a backend).
- Adoptar `application-{prod}.properties` con variables de entorno para credenciales, buckets y endpoints.
- Añadir health endpoints y métricas Prometheus (`/actuator/health`, `/actuator/prometheus`).

## Checklist mínimo antes del primer despliegue en producción

- [ ] Dockerfile optimizado y reproducible.
- [ ] CI/CD para build y despliegue a staging/prod.
- [ ] Object storage configurado y CDN en frente de los assets.
- [ ] `app.images.storage.provider=s3` configurado en producción.
- [ ] DB gestionada con migraciones versionadas.
- [ ] Redis para cache/sesiones y Message Broker para tareas.
- [ ] Monitoreo y alertas básicas configuradas.
- [ ] TLS + dominio + políticas de seguridad.
- [ ] Plan de backups y pruebas de restauración.

## Migración de imágenes (pasos operativos)

1. Provisionar bucket y CDN.
2. Copiar `data/images/*` al bucket (ej. `aws s3 sync data/images s3://pelisapp-images`).
3. Ejecutar script que actualice rutas en la BD o reemplace referencias.
4. Cambiar la configuración de la app para usar `S3ImageStorage`.
5. Probar en staging y habilitar CDN caching.

## Notas finales

Este plan está pensado para ser práctico y empezar por los puntos de mayor impacto: separar imágenes + CDN y contenerizar la app. Tras esto, habilitar escalado horizontal y observabilidad permitirá soportar cargas reales.

Si quieres, puedo:
- Generar plantillas de `Dockerfile`, `helm` chart y `GitHub Actions` para CI/CD.
- Escribir la interfaz `ImageStorage` y la implementación `S3ImageStorage` en Java y preparar las migraciones.
