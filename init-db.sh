#!/bin/bash
# Script para crear bases de datos adicionales en PostgreSQL al inicializar el contenedor.
# Se ejecuta automáticamente como parte de docker-entrypoint-initdb.d

set -e

# La base de datos principal (tourpresence) ya la crea POSTGRES_DB automáticamente.
# Aquí creamos keycloak_db si no existe.

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE keycloak_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak_db')\gexec
EOSQL

echo " Base de datos keycloak_db verificada/creada."