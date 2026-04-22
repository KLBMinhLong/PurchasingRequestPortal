# Frontend

Angular 21 workspace for Purchasing Request Portal.

## Run local

From `frontend` folder:

```bash
npm install
npm start
```

Default URL:

- `http://localhost:4200`

## Build

```bash
npm run build
```

Build output:

- `frontend/dist/purchasing-request-portal-frontend`

## Current app modules

- `core`: auth service, interceptor, guard, token storage
- `features/auth`: login page, forbidden page
- `features/user-management`: CRUD template calling backend APIs

## Notes

- Frontend only calls backend APIs (`http://localhost:8082/api/...`).
- Frontend does not connect directly to Keycloak.
