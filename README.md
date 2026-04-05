# HPS System

## Quick Start

```bash
./scripts/dev.sh start   # build + start (ephemeral DB)
./scripts/dev.sh stop    # tear down
./scripts/dev.sh logs    # tail logs
```

## Ports

| Service    | Dev  | Local |
|------------|------|-------|
| API        | 8080 | 8081  |
| Frontend   | 3000 | 3001  |
| Admin      | 3002 | 3003  |
| PostgreSQL | 5432 | 5433  |
| Typesense  | 8108 | 8109  |

## Scripts

- `scripts/dev.sh {start|stop|restart|logs}` — ephemeral dev environment (tmpfs DB)
- `scripts/local.sh {start|stop|restart|update|logs|reset}` — persistent local environment
- `scripts/build.sh` — build all Docker images (backend, frontend, admin)

## Default Credentials

- **Super Admin**: `admin@hps.local` / `admin1234`
