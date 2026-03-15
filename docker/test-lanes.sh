#!/usr/bin/env bash
set -euo pipefail

LANE="${1:-regression}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_DIR="$PROJECT_ROOT/docker"

case "$LANE" in
  smoke)
    GRADLE_TASK="cucumberSmokeTest"
    ;;
  kafka-producer)
    GRADLE_TASK="cucumberKafkaProducerTest"
    ;;
  kafka-consumer)
    GRADLE_TASK="cucumberKafkaConsumerTest"
    ;;
  regression)
    GRADLE_TASK="cucumberRegressionTest"
    ;;
  *)
    echo "Invalid lane: $LANE"
    echo "Usage: ./test-lanes.sh [smoke|kafka-producer|kafka-consumer|regression]"
    exit 1
    ;;
esac

cleanup() {
  cd "$DOCKER_DIR"
  ./test.sh down || echo "Warning: test infrastructure teardown failed" >&2
}
trap cleanup EXIT

cd "$DOCKER_DIR"
./test.sh up

cd "$PROJECT_ROOT"
./gradlew "$GRADLE_TASK"
