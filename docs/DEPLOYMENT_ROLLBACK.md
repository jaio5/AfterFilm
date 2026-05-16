Procedimiento de despliegue y rollback

Despliegue (Render)
1. Asegúrate de que `main` branch contiene los cambios y que `render.yaml` está en repo.
2. En Render, configura env vars (secrets) y pulsa `Manual Deploy` -> `Deploy latest commit`.
3. Observa logs y verifica `/actuator/health`.
4. Si todo OK, promociona a producción (si usas entornos separados), o configura un dominio.

Rollback
- Render permite desplegar una versión anterior desde el historial de deploys: en el panel de tu servicio -> Deploys -> seleccionar un deploy anterior -> `Revert to this deploy`.
- Alternativa CLI: `render deploy --service <YOUR_SERVICE> --commit <SHA>` para forzar despliegue de un commit específico.

Verificación post-rollback
- Repetir chequeos de salud y revisar logs.
- Si rollback afecta esquema de BD, restaura backup de Supabase (desde el panel de Supabase -> Backups) o ejecuta scripts de reversión.

Notas
- Mantén backups regulares de la base de datos (Supabase ofrece herramientas integradas).
- En producción preferible usar migraciones versionadas (Flyway) en lugar de `hibernate.ddl-auto=update`.

***
