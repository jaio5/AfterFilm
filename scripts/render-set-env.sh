#!/usr/bin/env bash
set -euo pipefail

echo "Este script muestra comandos render para establecer las env vars del servicio PelisApp-web."
echo "Instala render CLI y autentícate: https://render.com/docs/deploy-using-render-cli"
echo
echo "Sustituye <YOUR_SERVICE> por el nombre de tu servicio (p.ej. PelisApp-web)"
echo
cat <<'EOF'
# ejemplos (requieren render CLI y estar autenticado)
# Establecer variables obligatorias
render env set SPRING_PROFILES_ACTIVE=prod --service <YOUR_SERVICE>
render env set SPRING_DATASOURCE_URL='jdbc:postgresql://aws-1-eu-central-2.pooler.supabase.com:6543/postgres?sslmode=require' --service <YOUR_SERVICE>
render env set SPRING_DATASOURCE_USERNAME=postgres.nuewaowsrclicowxxpre --service <YOUR_SERVICE>
render env set SPRING_DATASOURCE_PASSWORD='<TU_PASSWORD_SUPABASE>' --service <YOUR_SERVICE> --secret
# Supabase storage settings
render env set APP_IMAGES_STORAGE_PROVIDER=supabase --service <YOUR_SERVICE>
render env set SUPABASE_URL='https://nuewaowsrclicowxxpre.supabase.co' --service <YOUR_SERVICE>
render env set SUPABASE_BUCKET='<TU_BUCKET>' --service <YOUR_SERVICE>
render env set SUPABASE_SERVICE_ROLE='<TU_SERVICE_ROLE_KEY>' --service <YOUR_SERVICE> --secret

# Application secrets
render env set APP_JWT_SECRET='<APP_JWT_SECRET_32plus>' --service <YOUR_SERVICE> --secret
render env set APP_ADMIN_PASSWORD='<ADMIN_PASSWORD>' --service <YOUR_SERVICE> --secret
render env set IMAGES_STORAGE_PROVIDER=supabase --service <YOUR_SERVICE>
render env set APP_IMAGES_STORAGE_PROVIDER=supabase --service <YOUR_SERVICE>
render env set JAVA_OPTS='-Xms256m -Xmx384m -XX:+UseG1GC' --service <YOUR_SERVICE>
EOF

echo
echo "Después de establecer las variables, despliega manualmente desde el panel o usa:"
echo "render deploy --service <YOUR_SERVICE>"
