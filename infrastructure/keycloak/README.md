# Keycloak Setup

This folder contains the dev bootstrap for Keycloak 26.1.0.

## What is imported

- Realm: `purchasing-portal`
- Realm roles: `ADMIN`, `USER`, `APPROVER`, `REQUESTOR`
- Client: `prp-backend`
- Client type: confidential
- Grant types: direct access grants and service account enabled

## How to start

1. Copy `infrastructure/env/.env.example` to `infrastructure/env/.env` or keep using the root `.env` file.
2. Make sure `KEYCLOAK_CLIENT_SECRET` matches the client secret in the realm import.
3. Start the stack:

```bash
docker compose -f infrastructure/docker-compose.yml up -d --force-recreate keycloak
```

## Initial access

- Admin console: `http://localhost:8080`
- Admin user: value from `KEYCLOAK_ADMIN`
- Admin password: value from `KEYCLOAK_ADMIN_PASSWORD`

## After first login

- Open realm `purchasing-portal`
- Verify client `prp-backend`
- Assign service-account roles from `realm-management` to the client service account:
  - `manage-users`
  - `query-users`
  - `view-users`
  - `view-realm`
  - `manage-realm` if you want the backend to manage realm settings

## Notes

- Frontend must not connect directly to Keycloak.
- All auth calls go through the backend.
- If you want to recreate the realm from scratch, remove the Postgres volume and restart the stack.
