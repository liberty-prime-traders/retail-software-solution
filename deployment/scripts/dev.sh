#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
ENV_FILE="deployment/env/dev.env"

if [[ "$ACTION" == "down" ]]; then
  docker compose -f deployment/compose/postgres-compose.yml --env-file "$ENV_FILE" down --remove-orphans
  docker compose -f deployment/compose/redis-compose.yml --env-file "$ENV_FILE" down --remove-orphans
else
  docker compose -f deployment/compose/postgres-compose.yml --env-file "$ENV_FILE" up -d
  docker compose -f deployment/compose/redis-compose.yml --env-file "$ENV_FILE" up -d
fi
