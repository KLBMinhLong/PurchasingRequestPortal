-- This script runs on first PostgreSQL initialization only.
-- It creates isolated databases and users for infrastructure services.

CREATE USER keycloak WITH PASSWORD 'keycloak_dev_password';
CREATE DATABASE keycloak OWNER keycloak;

CREATE USER camunda WITH PASSWORD 'camunda_dev_password';
CREATE DATABASE camunda OWNER camunda;
