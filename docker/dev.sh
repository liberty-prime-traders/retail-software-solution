#!/usr/bin/env bash

ACTION="${1:-up}"

if [[ "$ACTION" == "down" ]]; then
  docker compose -f postgres-compose.yml --env-file dev.env down
  docker compose -f kafka-compose.yml --env-file dev.env down
else
  docker compose -f postgres-compose.yml --env-file dev.env up -d
  docker compose -f kafka-compose.yml --env-file dev.env up -d
fi
