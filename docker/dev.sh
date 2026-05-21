#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
ENV_FILE="dev.env"

if [[ "$ACTION" == "down" ]]; then
  docker compose -f postgres-compose.yml --env-file "$ENV_FILE" down --remove-orphans
  docker compose -f kafka-compose.yml --env-file "$ENV_FILE" down --remove-orphans
  docker compose -f redis-compose.yml --env-file "$ENV_FILE" down --remove-orphans
else
  docker compose -f postgres-compose.yml --env-file "$ENV_FILE" up -d
  docker compose -f kafka-compose.yml --env-file "$ENV_FILE" up -d
  docker compose -f redis-compose.yml --env-file "$ENV_FILE" up -d
fi
