**Despliegue en Render (app Docker) + Supabase (Postgres)

Resumen rápido
- Backend: desplegar `PelisApp` en Render usando `render.yaml` o creando un servicio Web con Dockerfile.
- Base de datos: usar Supabase (plan Free) y conectar la URL Postgres a Render mediante variables de entorno.

1) Crear proyecto Supabase
- Regístrate en https://supabase.com (no requiere tarjeta). Crea un nuevo proyecto y toma la conexión Postgres (host, port, database, user, password). Copia la `DATABASE_URL` o los valores por separado.

2) Configurar variables de entorno en Render
- En tu dashboard de Render, crea un nuevo servicio usando el repositorio (o sube el `render.yaml`).
- En la sección de Environment -> Environment Variables añade las siguientes variables (marcar como "Secret" cuando corresponda):
  - `SPRING_DATASOURCE_HOST` = <supabase-host>
  - `SPRING_DATASOURCE_PORT` = <supabase-port> (5432)
  - `SPRING_DATASOURCE_DB` = <supabase-database>
  - `SPRING_DATASOURCE_USERNAME` = <supabase-user>
  - `SPRING_DATASOURCE_PASSWORD` = <supabase-password>
  - `SPRING_PROFILES_ACTIVE` = prod
  - `APP_JWT_SECRET` = <genera_un_secreto_largo>
  - `APP_IMAGES_STORAGE_PROVIDER` = local (o `s3` si usarás S3)
  - Si usas S3: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `IMAGES_S3_BUCKET`, `IMAGES_S3_REGION`

3) Ajustes JVM y Hikari (recomendado para free tiers)
- En Render, en la sección de Environment, añade también:
  - `JAVA_OPTS` = -Xms256m -Xmx384m -XX:+UseG1GC
  - `SPRING_DATASOURCE_MAX_POOL_SIZE` = 2
- En `application.properties` ya se mapean `SPRING_DATASOURCE_*`; opcionalmente configura Hikari pool size en `application-prod.properties`.

4) Construir y desplegar
- Render detectará tu `render.yaml` o puedes crear el servicio desde el repositorio GUI. El despliegue construirá usando tu `Dockerfile`.
- Comandos locales útiles para pruebas antes de push:

```bash
mvn -B -DskipTests package
docker build -t pelisapp:latest .
docker run -e SPRING_DATASOURCE_HOST=<host> -e SPRING_DATASOURCE_USERNAME=<user> -e SPRING_DATASOURCE_PASSWORD=<pw> -p 8080:8080 pelisapp:latest
```

5) Migraciones / datos iniciales
- `spring.jpa.hibernate.ddl-auto=update` hará las tablas necesarias, pero para producción es mejor usar Flyway/Liquibase.

6) Notas y recomendaciones
- Render free duerme tras inactividad; la primera petición puede tardar en arrancar.
- Supabase free es persistente y no requiere tarjeta. Configura backups si es crítico.
- No pongas secretos en el repositorio; usa el gestor de secrets de Render.

***
