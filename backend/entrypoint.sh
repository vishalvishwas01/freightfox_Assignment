#!/bin/sh
set -eu

# Render supplies a PostgreSQL URI; Spring's PostgreSQL driver expects a JDBC URL.
if [ -n "${DATABASE_URL:-}" ]; then
  database_uri="${DATABASE_URL#postgres://}"
  database_uri="${database_uri#postgresql://}"
  authority_and_path="${database_uri#*@}"
  host_and_port="${authority_and_path%%/*}"
  database_name="${authority_and_path#*/}"
  database_name="${database_name%%\?*}"
  db_host="${host_and_port%%:*}"
  db_port="${host_and_port#*:}"
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${db_host}:${db_port}/${database_name}"
fi

exec java -XX:MaxRAMPercentage=75 -jar /app/app.jar
