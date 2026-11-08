#!/usr/bin/env bash

ACTION="${1:-up}"  # default to up

COMPOSE=(docker compose -f deployment/compose/docker-compose.yml -f deployment/compose/docker-compose.debug.yml --env-file deployment/env/dev.env --env-file deployment/env/debug.env)

if [[ "$ACTION" == "down" ]]; then
  "${COMPOSE[@]}" down
else
  "${COMPOSE[@]}" up -d --no-deps rtss-server
fi
