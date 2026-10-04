#!/usr/bin/env bash
# Usage: ./restore-db.sh <source-postgres-container> <target-postgres-container>
# Copies the whole source database over the target. Stops/restarts only the
# target stack's rtss-server container (if it is running in Docker).
set -euo pipefail

PG_SERVICE=rtss-postgres
SERVER_SERVICE=rtss-server

SRC="${1:?usage: restore-db.sh <source-container> <target-container>}"
DST="${2:?usage: restore-db.sh <source-container> <target-container>}"

die() { echo "refusing: $*" >&2; exit 1; }
label() { docker inspect -f "{{ index .Config.Labels \"$2\" }}" "$1"; }
env_of() { docker exec "$1" printenv "$2"; }

[[ "$SRC" != "$DST" ]] || die "source and target are the same container"

for c in "$SRC" "$DST"; do
  [[ "$(label "$c" com.docker.compose.service)" == "$PG_SERVICE" ]] || die "$c is not a $PG_SERVICE container"
done

# The target must opt in via ALLOW_DB_OVERWRITE=true in its env file.
[[ "$(label "$DST" rtss.allow-db-overwrite)" == "true" ]] \
  || die "$DST does not have rtss.allow-db-overwrite=true"

DST_PROJECT="$(label "$DST" com.docker.compose.project)"

SRC_DB="$(env_of "$SRC" POSTGRES_DB)"; SRC_USER="$(env_of "$SRC" POSTGRES_USER)"
DST_DB="$(env_of "$DST" POSTGRES_DB)"; DST_USER="$(env_of "$DST" POSTGRES_USER)"

dst_psql() { docker exec -i "$DST" psql -U "$DST_USER" -d postgres -v ON_ERROR_STOP=1 "$@"; }

# Stop only the target stack's app server, if it runs in Docker.
SERVER="$(docker ps -q \
  --filter "label=com.docker.compose.project=$DST_PROJECT" \
  --filter "label=com.docker.compose.service=$SERVER_SERVICE")"
if [[ -n "$SERVER" ]]; then
  echo "Stopping $SERVER_SERVICE in $DST_PROJECT..."
  docker stop "$SERVER" >/dev/null
  trap 'echo "FAILED: $SERVER_SERVICE in $DST_PROJECT was left stopped." >&2' ERR
fi

echo "Checking for open connections to '$DST_DB'..."
CONNS="$(dst_psql -tA -F' | ' -c \
  "SELECT coalesce(nullif(application_name,''),'(no app name)'), coalesce(client_addr::text,'local')
     FROM pg_stat_activity WHERE datname = '$DST_DB' AND pid <> pg_backend_pid()")"
if [[ -n "$CONNS" ]]; then
  echo "refusing: target database has open connections:" >&2
  echo "$CONNS" >&2
  [[ -z "$SERVER" ]] || echo "(note: $SERVER_SERVICE was stopped and is NOT restarted)" >&2
  exit 2
fi

echo "Recreating '$DST_DB'..."
dst_psql -c "DROP DATABASE IF EXISTS \"$DST_DB\""
dst_psql -c "CREATE DATABASE \"$DST_DB\" OWNER \"$DST_USER\""

echo "Copying $SRC -> $DST..."
docker exec "$SRC" pg_dump -Fc -U "$SRC_USER" -d "$SRC_DB" \
  | docker exec -i "$DST" pg_restore --no-owner --no-privileges -U "$DST_USER" -d "$DST_DB"

if [[ -n "$SERVER" ]]; then
  trap - ERR
  echo "Restarting $SERVER_SERVICE..."
  docker start "$SERVER" >/dev/null
fi
echo "Done."
