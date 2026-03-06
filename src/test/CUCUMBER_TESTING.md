# Cucumber E2E

## Purpose
Run HTTP-level E2E tests with real Postgres and Kafka, isolated from dev/prod.

## Infra
- Compose file: `docker/docker-compose.test.yml`
- Env file: `docker/test.properties`
- Helper script: `docker/test.sh`
- Postgres: `localhost:5435` (`rtss_e2e_test`)
- Kafka: `localhost:9095`

## Run
```bash
cd docker && ./test.sh up
cd .. && ./gradlew cucumberRegressionTest
cd docker && ./test.sh down
```

## Lane Runner Script
```bash
cd docker && ./test-lanes.sh smoke
cd docker && ./test-lanes.sh kafka
cd docker && ./test-lanes.sh kafka-consumer
cd docker && ./test-lanes.sh regression
```

## Lanes
- `./gradlew cucumberSmokeTest`
- `./gradlew cucumberKafkaTest`
- `./gradlew cucumberKafkaConsumerTest`
- `./gradlew cucumberRegressionTest`

## HTML Reports
- General test report: `build/reports/tests/test/index.html`
- Smoke lane: `build/reports/tests/cucumberSmokeTest/index.html`
- Kafka producer lane: `build/reports/tests/cucumberKafkaTest/index.html`
- Kafka consumer lane: `build/reports/tests/cucumberKafkaConsumerTest/index.html`
- Regression lane: `build/reports/tests/cucumberRegressionTest/index.html`
- Cucumber plugin report: `build/reports/cucumber.html`

## Test Structure
- Runner: `src/test/kotlin/.../cucumber/RunCucumberTest.kt`
- Features: `src/test/resources/features/**`
- Steps: `src/test/kotlin/.../cucumber/steps/**`
- Hooks: `src/test/kotlin/.../cucumber/hooks/TestHooks.kt`
- Shared request/auth: `src/test/kotlin/.../cucumber/config/AuthenticatedRequestFactory.kt`
- Fixture builder: `src/test/kotlin/.../cucumber/config/TestFixtureBuilder.kt`
- DB cleanup: `src/test/kotlin/.../cucumber/config/TestDatabaseCleaner.kt`

## Tagging
- `@smoke`: critical-path checks only (small, fast subset)
- `@negative`: authorization/validation failure paths
- Domain tags: `@products`, `@organizations`
- Integration tags: `@kafka`
- `@kafka-consumer`: validates listener-side consumption and sync side effects

## Auth Model In Tests
- `mock-platform-admin-token` -> platform admin roles
- `mock-org-admin-token` -> org-admin-like role set
- `mock-user-token` -> authenticated user with no admin role

## Notes
- `@Before` hook truncates test tables (excluding `platform` schema and Liquibase tables).
- Product test FK values are seeded from fixtures, not random UUIDs.
- `@kafka-consumer` hook resets `catalog-sync-group` offsets to latest and starts listeners.
