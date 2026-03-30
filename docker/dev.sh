#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
MODE="${2:-broker}"
ENV_FILE="dev.env"

if [[ "$ACTION" == "down" ]]; then
  docker compose -f postgres-compose.yml --env-file "$ENV_FILE" down --remove-orphans
  docker compose -f kafka-compose.yml --env-file "$ENV_FILE" down --remove-orphans
else
  docker compose -f postgres-compose.yml --env-file "$ENV_FILE" up -d
  if [[ "$MODE" == "ui" ]]; then
    docker compose -f kafka-compose.yml --env-file "$ENV_FILE" --profile ui up -d
  else
    docker compose -f kafka-compose.yml --env-file "$ENV_FILE"up -d rtss-kafka
  fi
fi
