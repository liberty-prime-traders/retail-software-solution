# Cucumber E2E

## Purpose
HTTP-level E2E tests against a real Postgres and Kafka stack, isolated from dev/prod.

---

## Running Tests

Postgres and Kafka are managed automatically by Testcontainers with container reuse enabled. Docker Desktop must be running before starting any tests.

```bash
./cucumber.sh              # all tests (regression lane)
./cucumber.sh smoke
./cucumber.sh kafka-producer
./cucumber.sh kafka-consumer
```

Individual scenarios can also be run directly from the IDE by clicking the run button next to a scenario in any feature file.

Reports land in `build/reports/tests/{lane}/index.html` and `build/reports/cucumber.html`.

---

## Lanes and Tags

Each lane is a Gradle task that filters by tag. Tag scenarios to control which lanes they appear in.

| Lane             | Gradle Task                 | Run when                                                        |
|------------------|-----------------------------|-----------------------------------------------------------------|
| `smoke`          | `cucumberSmokeTest`         | `@smoke and not @ignore`                                        |
| `kafka-producer` | `cucumberKafkaProducerTest` | `@publishes-to-kafka and not @consumes-from-kafka and not @ignore` |
| `kafka-consumer` | `cucumberKafkaConsumerTest` | `@consumes-from-kafka and not @ignore`                          |
| `regression`     | `cucumberRegressionTest`    | `not @ignore`                                                   |

**`@publishes-to-kafka`** — tests that an action publishes the right event to the topic. Before the scenario a Kafka consumer subscribes from latest; the step polls and asserts message shape. The application's own listeners are not running.

**`@consumes-from-kafka`** — tests the full event pipeline. The application's listeners are started before the scenario, consumer offsets are reset to latest, and the test asserts the downstream effect (e.g. a product was synced to the location catalog). Slower and heavier than publish tests.

Other tags: `@ignore` to skip everywhere.

---

## Boilerplate Session

`BoilerPlateDataInitializer` runs once after Spring starts (before any scenario). It bootstraps a complete org/location pair through the real API and stores them in `BoilerPlateSessionContextHolder` and `InjectContext` (`PersistentKey.ORGANIZATION`).

This means every scenario starts with a real organization and location already in place. Steps that need to reference them use the `#organization` and `#location->0` placeholders.

---

## Authentication

Steps set one of two mock tokens. The security filter maps the token to a principal and roles — no real Okta involved.

```gherkin
Given I am authenticated as an organization user    # ORG_USER token, org-level access
Given I am not authenticated                        # no token, expects 403
```

An auth step must appear before any step that creates fixture data. Fixture creation calls the real API and will fail with a 401 if no token is set.

The `AuthContext` is initialized to `PLATFORM_ADMIN` token before every scenario by `TestHooks`. Auth steps override this.

---

## Fixtures

Fixture steps create prerequisite data through the real API and store the resulting ID in `InjectContext`. They use dedicated fixture builder classes rather than inline API calls.

```gherkin
Given a category exists
Given a product group exists
Given a unit group exists
Given a unit exists
Given a location context exists
```

Each step chains off the previous — `product group exists` reads `CATEGORY` from `InjectContext`, `unit exists` reads `UNIT_GROUP`, etc. Steps must appear in dependency order.

For batch product creation:
```gherkin
Given the following products exist:
  | productName | description  |
  | Widget A    | First widget |
  | Widget B    | Second widget|
```

The step resolves `PRODUCT_GROUP` and `UNIT_VALUE` from `InjectContext` and merges them into each row. Feature files describe business data only — never IDs.

---

## Cross-Step Data (`InjectContext`)

`InjectContext` has two stores:

- **Transient** (`TransientKey`) — cleared before every scenario
- **Persistent** (`PersistentKey`) — survives for the entire test session

When a step creates something, it stores the resulting ID under its key. Later steps reference it with `#keyname` (last added) or `#keyname->index` (0-based).

**TransientKey names:** `product`, `category`, `product_group`, `unit_group`, `unit_value`, `location`

**PersistentKey names:** `organization`

```gherkin
# #product->0 is the first created product, #product->1 is the second
Given the following products exist:
  | productName | description |
  | Widget A    | first       |
  | Widget B    | second      |

When I send a DELETE request to "/secured/products/#product->1"
```

The `AuthenticatedRequestFactory` automatically attaches `X-Organization-Id` and `X-Location-Id` headers from `InjectContext` on every request — no manual header setup needed.

---

## DataTable to DTO

Column names must match DTO field names exactly. `DtoConverter` handles conversion and resolves `#key->index` placeholders before Jackson parses.

Special cell values:
- `NULL` → `null`
- `NONE` or `DEFAULT` (JSON only) → empty object, uses DTO defaults

```gherkin
Given the following products exist:
  | productName | description | tagsToAdd |
  | Widget      | A widget    | NULL      |
```

---

## Generic Request Steps

For scenarios that test an endpoint directly without needing a domain step:
```gherkin
When I send a GET request to "/secured/organizations"
When I send a POST request to "/secured/product-category" with body:
  """
  { "categoryName": "Electronics" }
  """
When I send a PUT request to "/secured/products" with body:
  """
  { "id": "#product->0", "productName": "Updated Widget" }
  """
When I send a DELETE request to "/secured/products/#product->0"
```

Endpoints and bodies are resolved through `InjectContext` before the request fires.

---

## Response Assertions

```gherkin
Then the response status should be 200
Then the response should contain field "id"
Then the response field "productName" should be "Widget"
Then the response should contain 3 items
Then the response should be an empty list
Then the response error should contain "not found"
Then the response error field "message" should be "Duplicate name"
```

`lastError` is available whenever the response status is ≥ 400 — no extra setup needed.

---

## Kafka Steps

**Publisher assertion** (use with `@publishes-to-kafka`):
```gherkin
Then a catalog event should be published for table "organization_product"
And the catalog event should reference the created resource
```

The first step polls the topic for up to 15 s and stores the matched event in `KafkaContext`. The second compares the event's `entityId` to the response ID from the previous create call.

**Consumer sync assertion** (use with `@consumes-from-kafka`):
```gherkin
Then the location catalog should contain product "Widget A"
```

Polls the location-products search endpoint for up to 20 s waiting for async Kafka consumption to complete.

---

## Scenario Lifecycle

Before every scenario:
1. All test tables truncated (`RESTART IDENTITY CASCADE`) — platform schema and Liquibase tables excluded
2. Auth, response, inject (transient), and Kafka contexts reset
3. Organization/location-scoped caches cleared
4. `AuthContext` initialized to `PLATFORM_ADMIN`

For `@publishes-to-kafka` scenarios, before the scenario also:
1. Subscribes a Kafka consumer to `CATALOG_EVENTS` topic from latest offset

For `@consumes-from-kafka` scenarios, before the scenario also:
1. Resets `catalog-sync-group` offsets to latest (skips any backlog from previous runs)
2. Starts the application's Kafka listener containers

After `@publishes-to-kafka` scenarios: catalog events consumer is closed.

After `@consumes-from-kafka` scenarios: listeners are stopped so they don't bleed into the next scenario.

Kafka polling timeouts: 15 s for published events, 20 s for consumer sync effects.
