Pasos finales para dejar `PelisApp` desplegada en Render (usando Supabase)

1) Confirmar que el repo tiene estos archivos:
   - `Dockerfile` (ya presente)
   - `render.yaml` (ya presente)
   - `application-prod.properties` (perfil `prod` configurado)
   - `.dockerignore` (ya presente)

2) En Render (panel):
   - `New +` → `Web Service` → conecta tu repo.
   - Asegúrate de seleccionar `Docker` y que `dockerfilePath` apunte a `Dockerfile`.
   - Branch: `main`.
   - Plan: `Free`.
   - Health check path: `/actuator/health`.

3) Establecer variables de entorno (dashboard -> Environment):
   - `SPRING_PROFILES_ACTIVE=prod`
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-eu-central-2.pooler.supabase.com:6543/postgres?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME=postgres.nuewaowsrclicowxxpre`
   - `SPRING_DATASOURCE_PASSWORD=<TU_PASSWORD_SUPABASE>` (marcar secret)
   - `APP_JWT_SECRET=<secreto_32+>` (marcar secret) 
   - `APP_ADMIN_PASSWORD=<password_admin>` (marcar secret)
   - `IMAGES_STORAGE_PROVIDER=local`
   - `JAVA_OPTS=-Xms256m -Xmx384m -XX:+UseG1GC`

4) (Opcional) Usar Render CLI para inyectar env vars rápidamente:
   - Instala: `curl -sL https://cdn.render.com/cli/install.sh | bash`
   - Autentica: `render login`
   - Ejecuta: `./scripts/render-set-env.sh` (ajusta `<YOUR_SERVICE>` y los secretos)

5) Desplegar:
   - Desde el panel: Manual Deploy → Deploy Latest Commit
   - O con CLI: `render deploy --service <YOUR_SERVICE>`

6) Verificar:
   - Logs en Render (pestaña Logs)
   - Health: `curl -sS https://<tu-servicio>.onrender.com/actuator/health`

7) Notas de seguridad:
   - Rota la contraseña de Supabase después de la configuración si la has compartido.
   - Considera usar S3 para imágenes en producción y configurar `IMAGES_STORAGE_PROVIDER=s3` y las credenciales `AWS_*` en Render.

8) Si hay problemas de conexión:
   - Usa el Session Pooler de Supabase y pon su host/port en `SPRING_DATASOURCE_URL`.


Listo — cuando quieras puedo ejecutar `./scripts/render-set-env.sh` localmente (necesitas autenticar `render`) o guiarte paso a paso en el panel de Render mientras pegas las variables.
