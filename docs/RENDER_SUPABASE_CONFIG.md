Configuración necesaria para conectar `PelisApp` (Render) con una base de datos Supabase

1) Variables de entorno requeridas en Render (marcar como secret cuando corresponda)

- `SPRING_DATASOURCE_HOST` -> Host de la conexión Postgres (p.ej. db.abcd.supabase.co)
- `SPRING_DATASOURCE_PORT` -> 5432
- `SPRING_DATASOURCE_DB` -> nombre de la base de datos (proporcionado por Supabase)
- `SPRING_DATASOURCE_USERNAME` -> user
- `SPRING_DATASOURCE_PASSWORD` -> password
- `SPRING_PROFILES_ACTIVE` -> prod
- `APP_JWT_SECRET` -> secreto JWT largo (>=32 chars)
- `APP_IMAGES_STORAGE_PROVIDER` -> local | s3

Opcionales para emails / S3
- `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `IMAGES_S3_BUCKET`, `IMAGES_S3_REGION`

2) Cómo obtener credenciales en Supabase

- En el panel del proyecto Supabase, ve a Settings -> Database -> Connection string. Copia host, database, user y password.

3) Probar localmente con Supabase (script)

Guarda las credenciales en variables y ejecuta el contenedor localmente para probar antes de desplegar:

```bash
# Ejemplo (rellena con tus valores reales)
export SPRING_DATASOURCE_HOST=db.abcd.supabase.co
export SPRING_DATASOURCE_PORT=5432
export SPRING_DATASOURCE_DB=postgres
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=mi_password_segura
export APP_JWT_SECRET="$(openssl rand -hex 32)"
export JAVA_OPTS="-Xms256m -Xmx384m -XX:+UseG1GC"

# Construir jar local
mvn -B -DskipTests package

# Ejecutar jar
java $JAVA_OPTS -jar target/*.jar
```

4) Notas sobre migraciones

- `spring.jpa.hibernate.ddl-auto=update` crea/ajusta tablas automáticamente para desarrollo. Para producción, usar Flyway o Liquibase y ejecutar migraciones controladas.

5) Despliegue en Render

- En Render crea servicio Web (tipo Docker) usando `render.yaml` o el wizard; añade las env vars listadas arriba; despliega.

6) Verificación

- Tras desplegar, revisa los logs en Render. Accede a `/actuator/health` para comprobar estado.
