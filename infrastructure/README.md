# Infrastructure

## Env templates

- env/.env.example
- env/backend.env.example
- env/frontend.env.example

## Compose files

- docker-compose.yml
- compose/docker-compose.dev.yml
- compose/docker-compose.test.yml
- compose/docker-compose.prod.yml

## Run with root .env

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

## Run with custom env file

```bash
docker compose --env-file infrastructure/env/.env -f infrastructure/docker-compose.yml up -d
```

## Run with overlays

```bash
docker compose --env-file infrastructure/env/.env -f infrastructure/docker-compose.yml -f infrastructure/compose/docker-compose.dev.yml up -d
```
