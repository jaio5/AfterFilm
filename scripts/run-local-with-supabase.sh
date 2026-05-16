#!/usr/bin/env bash
set -euo pipefail

if [ -z "${SPRING_DATASOURCE_HOST:-}" ]; then
  echo "Please set SPRING_DATASOURCE_HOST, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD"
  exit 1
fi

JAVA_OPTS=${JAVA_OPTS:-"-Xms256m -Xmx384m -XX:+UseG1GC"}

echo "Building project..."
mvn -B -DskipTests package

echo "Running jar with Supabase connection to $SPRING_DATASOURCE_HOST..."
exec java $JAVA_OPTS -jar target/*.jar
