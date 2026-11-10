#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"  # default to up
ENV_FILE="deployment/env/prod.env"
COMPOSE=(docker compose -f deployment/compose/docker-compose.yml --env-file "$ENV_FILE")

APP_VERSION="$(grep -E '^APP_VERSION=' "$ENV_FILE" | cut -d= -f2)"
if [[ ! "$APP_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "APP_VERSION must be a pinned release tag such as 1.2.3, not '$APP_VERSION'." >&2
  exit 1
fi

if [[ "$ACTION" == "down" ]]; then
  "${COMPOSE[@]}" down
elif [[ "$ACTION" == "server" ]]; then
  "${COMPOSE[@]}" up -d --no-deps rtss-server
else
  "${COMPOSE[@]}" up -d
fi
