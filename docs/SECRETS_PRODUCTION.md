Gestión de secretos y hardening para PelisApp (producción)

Resumen:
- Para Render: usa "Environment -> Environment Variables" y marca secrets como secret.
- Rota la contraseña de Supabase después de desplegar si la compartiste.
- Evita poner valores sensibles en el repositorio.

Opciones avanzadas de gestión de secretos:
- **Render secrets**: suficiente para deploys sencillos.
- **AWS Secrets Manager** o **HashiCorp Vault**: para rotación automática y control centralizado.

Recomendaciones de hardening:
- TLS siempre: `sslmode=require` en la URL de Postgres.
- No exponer actuators sensibles: `management.endpoints.web.exposure.include=health,info` (ya aplicado).
- Desactivar `spring.jpa.hibernate.ddl-auto=update` en producción y usar Flyway/Liquibase.
- Limitar tamaño de pool Hikari en env vars para entornos pequeños.
- Habilitar CSP y X-Content-Type-Options en la configuración del servidor si sirves frontend.
- Registrar accesos críticos y activar alertas si el endpoint de salud reporta down.

Integración con AWS Secrets Manager (resumen):
- Añade la dependencia `software.amazon.awssdk:secretsmanager` si quieres que la app lea secretos en runtime.
- Configura un role IAM en el entorno (ECS/EKS) o credenciales en Render (no recomendado). Ejemplo: `SecretsManagerClient` y leer `getSecretValue` al arranque.

***
