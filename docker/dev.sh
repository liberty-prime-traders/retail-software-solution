#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
MODE="${2:-broker}"

if [[ "$ACTION" == "down" ]]; then
  docker compose -f postgres-compose.yml --env-file dev.env down --remove-orphans
  docker compose -f kafka-compose.yml --env-file dev.env down --remove-orphans
else
  docker compose -f postgres-compose.yml --env-file dev.env up -d
  if [[ "$MODE" == "ui" ]]; then
    docker compose -f kafka-compose.yml --env-file dev.env --profile ui up -d
  else
    docker compose -f kafka-compose.yml --env-file dev.env up -d rtss-kafka
  fi
fi
