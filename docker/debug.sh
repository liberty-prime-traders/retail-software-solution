#!/usr/bin/env bash

ACTION="${1:-up}"  # default to up

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.debug.yml --env-file dev.env --env-file debug.env)

if [[ "$ACTION" == "down" ]]; then
  "${COMPOSE[@]}" down
else
  "${COMPOSE[@]}" up -d --no-deps rtss-server
fi
