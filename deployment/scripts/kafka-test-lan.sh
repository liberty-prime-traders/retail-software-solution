#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
COMPOSE=(docker compose -f deployment/compose/kafka-compose.yml -f deployment/compose/kafka-test-lan.yml --env-file deployment/env/test.env --env-file deployment/env/test-lan-kafka.env)

docker network inspect ezra-home > /dev/null 2>&1 || docker network create ezra-home > /dev/null

if [[ "$ACTION" == "down" ]]; then
  "${COMPOSE[@]}" down
else
  "${COMPOSE[@]}" up -d
fi
