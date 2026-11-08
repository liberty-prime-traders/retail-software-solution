#!/usr/bin/env bash

ACTION="${1:-up}"  # default to up
COMPOSE=(docker compose -f deployment/compose/docker-compose.yml --env-file deployment/env/test.env)
LAN_KAFKA_COMPOSE=("${COMPOSE[@]}" --env-file deployment/env/test-lan-kafka.env)

if [[ "$ACTION" == "down" ]]; then
  "${COMPOSE[@]}" down
elif [[ "$ACTION" == "server" ]]; then
  "${COMPOSE[@]}" up -d --no-deps rtss-server
elif [[ "$ACTION" == "no-kafka" ]]; then
  # Shell env overrides the env file; --no-deps keeps rtss-server from pulling in rtss-kafka.
  KAFKA_ENABLED=false "${COMPOSE[@]}" up -d rtss-postgres rtss-redis
  KAFKA_ENABLED=false "${COMPOSE[@]}" up -d --no-deps rtss-server
elif [[ "$ACTION" == "lan-kafka" ]]; then
  "${LAN_KAFKA_COMPOSE[@]}" up -d rtss-postgres rtss-redis
  "${LAN_KAFKA_COMPOSE[@]}" up -d --no-deps rtss-server
else
  "${COMPOSE[@]}" up -d
fi
