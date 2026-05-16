**Secrets para hosts gratuitos (Vercel / Fly.io / Render / Netlify)**

- **Resumen:** Los hosts gratuitos o con capa gratuita normalmente ofrecen un gestor de variables de entorno en su dashboard. Para aplicaciones Java/Spring Boot lo óptimo es usar esas variables de entorno en lugar de archivos con credenciales.

- **Recomendación general:**
  - Mantén las credenciales en variables de entorno del propio servicio (ej. `SPRING_DATASOURCE_PASSWORD`, `APP_IMAGES_STORAGE_PROVIDER`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`).
  - Evita incluir credenciales en `application.properties` dentro del repositorio. Usa `SPRING_APPLICATION_JSON` o nombres de propiedades que Spring mapeará automáticamente desde el entorno.

- **Plataformas y notas prácticas:**
  - **Vercel / Netlify**: pensadas para frontend y funciones serverless; no son adecuadas para un backend Spring Boot tradicional. Si alojas sólo el frontend en Vercel, configura allí las env vars para la parte cliente (p.ej. `NEXT_PUBLIC_API_URL`) y despliega el backend en otra plataforma.
  - **Fly.io**: admite imágenes Docker y tiene un plan gratuito para pequeñas apps. Usa `fly secrets set KEY=VALUE` para inyectar secretos en runtime; Spring Boot los leerá como variables de entorno.
  - **Render**: permite Web Services con plan gratuito (sleep). Configura env vars en el dashboard de servicio.

- **Healthcheck**
  - Muchos hosts intentan verificar `/` o un `HEALTHCHECK` definido en la imagen. Hemos añadido la propiedad `app.allow-healthcheck` (por defecto `true`) para permitir `/actuator/health` sin autenticación cuando sea necesario para plataformas que necesitan un endpoint abierto.

- **Cómo configurar en tu plataforma (ejemplo Fly.io)**
  1. Instala `flyctl` y crea una app: `flyctl launch`.
  2. Añade secretos: `fly secrets set SPRING_DATASOURCE_PASSWORD=secret DB_USER=pelisapp`.
  3. Despliega: `fly deploy`.

- **Recomendación final:** Para un despliegue barato y sencillo recomiendo usar **Fly.io** (backend por contenedor) + **Vercel** para el frontend (Next.js). Si más adelante migras a AWS, cambia a AWS Secrets Manager y roles IAM.
