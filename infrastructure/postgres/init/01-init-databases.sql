-- This script runs on first PostgreSQL initialization only.
-- It creates isolated databases and users for infrastructure services.

CREATE USER keycloak WITH PASSWORD 'keycloak_dev_password';
CREATE DATABASE keycloak OWNER keycloak;

CREATE USER keycloak_spi WITH PASSWORD 'keycloak_spi_dev_password';

GRANT CONNECT ON DATABASE portal TO keycloak_spi;

CREATE USER camunda WITH PASSWORD 'camunda_dev_password';
CREATE DATABASE camunda OWNER camunda;

\connect portal

GRANT USAGE ON SCHEMA identity TO keycloak_spi;
GRANT SELECT ON ALL TABLES IN SCHEMA identity TO keycloak_spi;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity GRANT SELECT ON TABLES TO keycloak_spi;
