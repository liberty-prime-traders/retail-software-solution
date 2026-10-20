#!/usr/bin/env bash

ACTION="${1:-up}"  # default to up

if [[ "$ACTION" == "down" ]]; then
  docker compose --env-file test.env down
elif [[ "$ACTION" == "server" ]]; then
  docker compose --env-file test.env up -d --no-deps rtss-server
else
  docker compose --env-file test.env up -d
fi
