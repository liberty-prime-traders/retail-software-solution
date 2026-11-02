#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"  # default to up
ENV_FILE="prod.env"

APP_VERSION="$(grep -E '^APP_VERSION=' "$ENV_FILE" | cut -d= -f2)"
if [[ "$APP_VERSION" =~ ^(dev|test|latest)$ ]]; then
  echo "APP_VERSION must be a pinned release tag, not '$APP_VERSION'." >&2
  exit 1
fi

if [[ "$ACTION" == "down" ]]; then
  docker compose --env-file "$ENV_FILE" down
elif [[ "$ACTION" == "server" ]]; then
  docker compose --env-file "$ENV_FILE" up -d --no-deps rtss-server
else
  docker compose --env-file "$ENV_FILE" up -d
fi
