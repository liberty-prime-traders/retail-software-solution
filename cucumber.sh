#!/usr/bin/env bash
set -euo pipefail

LANE="${1:-regression}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$LANE" in
  smoke)          GRADLE_TASK="cucumberSmokeTest" ;;
  kafka-producer) GRADLE_TASK="cucumberKafkaProducerTest" ;;
  kafka-consumer) GRADLE_TASK="cucumberKafkaConsumerTest" ;;
  regression)     GRADLE_TASK="cucumberRegressionTest" ;;
  *)
    echo "Usage: ./cucumber.sh [smoke|kafka-producer|kafka-consumer|regression]"
    exit 1
    ;;
esac

cd "$PROJECT_ROOT"
./gradlew "$GRADLE_TASK"
