#!/usr/bin/env bash

ACTION="${1:-up}"  # default to up

if [[ "$ACTION" == "down" ]]; then
  docker compose --env-file test.env down
else
  docker compose --env-file test.env up -d
fi
