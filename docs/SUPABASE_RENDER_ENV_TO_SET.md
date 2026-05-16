Checklist de variables de entorno para Render (cópialas en el dashboard del servicio)

Variables obligatorias (reemplaza los valores):

- Opción recomendada (una sola URL):
	- `SPRING_DATASOURCE_URL` = `jdbc:postgresql://aws-1-eu-central-2.pooler.supabase.com:6543/postgres?sslmode=require`
	- `SPRING_DATASOURCE_USERNAME` = `postgres.nuewaowsrclicowxxpre`
	- `SPRING_DATASOURCE_PASSWORD` = `<TU_PASSWORD_SUPABASE>`

- Opción por campos (equivalente):

- `SPRING_DATASOURCE_HOST` = <tu-supabase-host>    # p.ej. db.abcd.supabase.co
- `SPRING_DATASOURCE_PORT` = 5432
- `SPRING_DATASOURCE_DB` = <tu-database>
- `SPRING_DATASOURCE_USERNAME` = <tu-username>
- `SPRING_DATASOURCE_PASSWORD` = <tu-password>
- `SPRING_PROFILES_ACTIVE` = prod
- `APP_JWT_SECRET` = <generar-secreto-largo-32+>
- `APP_ADMIN_PASSWORD` = <contrasena-admin-inicial>
- `APP_IMAGES_STORAGE_PROVIDER` = local
 - `APP_IMAGES_STORAGE_PROVIDER` = supabase

Supabase Storage (si vas a usar el bucket que creaste):

- `SUPABASE_URL` = `https://nuewaowsrclicowxxpre.supabase.co`
- `SUPABASE_BUCKET` = `<TU_BUCKET>`
- `SUPABASE_SERVICE_ROLE` = `<TU_SERVICE_ROLE_KEY>` (marca como secreto)

Recomendadas / opcionales:

- `JAVA_OPTS` = -Xms256m -Xmx384m -XX:+UseG1GC
- `SPRING_DATASOURCE_MAX_POOL_SIZE` = 2
- `MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_PORT` (si quieres enviar emails)
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `IMAGES_S3_BUCKET`, `IMAGES_S3_REGION` (si vas a usar S3)

Pasos rápidos para configurar en Render (panel web):

1. Accede a tu servicio en Render -> Environment -> Environment Variables.
2. Añade las variables obligatorias listadas arriba como "Environment Variables" (marcar como secret las contraseñas/secretos).
3. Guarda y despliega (o trigger manual de deploy).

Comprobación post-deploy:

```bash
curl -sS https://<tu-servicio>.onrender.com/actuator/health | jq
```

Si devuelve `{"status":"UP"}` la app está conectada correctamente.

Nota sobre la base de datos:
- Supabase provee una URL de conexión JDBC tipo: `postgres://user:password@host:5432/dbname`.
- Si deseas ejecutar SQL inicial (seed), puedes usar el SQL Editor de Supabase o ejecutar `psql` localmente apuntando al host de Supabase.

Importante sobre conectividad:
- Si Render no consigue conectar al host `db...supabase.co`, usa el **Session Pooler** de Supabase (host/puerto pooler) y colócalo en `SPRING_DATASOURCE_URL`.

Crear usuario admin manualmente (opcional):

- Puedes insertar un usuario admin en la BD vía SQL (ajusta nombres de tabla/columnas según el esquema):

```sql
INSERT INTO users (username, password_hash, roles, enabled, email) VALUES
('admin', '<bcrypt-hash-de-la-contrasena>', 'ROLE_ADMIN', true, 'tu@correo');
```

Usa bcrypt para generar el hash de la contraseña o registrar el usuario por la UI y luego asignarle rol ADMIN.
