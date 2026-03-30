#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
COMPOSE_FILE="docker-compose.test.yml"
ENV_FILE="test.env"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-rtss-e2e}"

if [[ "$ACTION" == "down" ]]; then
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
else
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
fi
