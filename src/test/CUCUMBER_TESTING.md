# Cucumber E2E

## Purpose
HTTP-level E2E tests against a real Postgres and Kafka stack, isolated from dev/prod.

---

## Configuration

Test configuration is externalized in `docker/test.properties`. This file contains all environment-specific values used by `application-test.yml`.

**Required properties:**
```properties
# Database
TEST_DB_HOST=localhost
TEST_DB_PORT=5435
TEST_DB_NAME=rtss_e2e_test
TEST_DB_USERNAME=rtss_test_user
TEST_DB_PASSWORD=rtss_test_password

# Kafka
TEST_KAFKA_BOOTSTRAP_SERVERS=localhost:9095

# Okta (mock values for tests)
TEST_OKTA_ISSUER=http://localhost:8080
TEST_OKTA_CLIENT_ID=test-client-id
TEST_OKTA_CLIENT_SECRET=test-client-secret
TEST_OKTA_ORG_URL=http://localhost:8080
TEST_OKTA_TOKEN=test-token
```

The `application-test.yml` references these properties using `${TEST_*}` placeholders. This keeps test configuration separate from the test code and makes it easy to adjust for different environments.

---

## Running Tests

```bash
# Start infrastructure, run a lane, tear down
cd docker && ./test-lanes.sh smoke
cd docker && ./test-lanes.sh kafka-producer
cd docker && ./test-lanes.sh kafka-consumer
cd docker && ./test-lanes.sh regression

# Or manually
cd docker && ./test.sh up
./gradlew cucumberRegressionTest
cd docker && ./test.sh down
```

Infrastructure: Postgres `localhost:5435` (`rtss_e2e_test`), Kafka `localhost:9095`.

Reports land in `build/reports/tests/{lane}/index.html` and `build/reports/cucumber.html`.

---

## Lanes and Tags

Each lane is a Gradle task that filters by tag. Tag scenarios to control which lanes they appear in.

| Lane | Gradle Task | Run when |
|---|---|---|
| `smoke` | `cucumberSmokeTest` | `@smoke` |
| `kafka-producer` | `cucumberKafkaProducerTest` | `@kafka-producer` (excludes `@kafka-consumer`) |
| `kafka-consumer` | `cucumberKafkaConsumerTest` | `@kafka-consumer` |
| `regression` | `cucumberRegressionTest` | everything except `@ignore` |

**`@kafka-producer`** — tests that an action publishes the right event to the topic. The step manually creates a Kafka consumer, polls the topic, and asserts the message shape. The application's own listeners are not running.

**`@kafka-consumer`** — tests the full event pipeline. The application's listeners are started before the scenario, consumer offsets are reset to latest, and the test asserts the downstream effect (e.g. a product was synced to the location catalog). Slower and heavier than producer tests.

Annotate a scenario with both if it needs to verify both sides:
```gherkin
@products @kafka-producer @kafka-consumer
Scenario: Product creation is consumed and synced to location catalog
```

Other tags: `@ignore` to skip everywhere.

---

## Authentication

Steps set one of three mock tokens. The security filter maps the token to a principal and roles — no real Okta involved.

```gherkin
Given I am authenticated as an organization user    # can create products, cannot manage orgs
Given I am authenticated as an organization admin   # org management rights
Given I am authenticated as a platform admin        # cross-org access
Given I am not authenticated                        # expects 403
```

All authenticated steps also seed `currentOrganizationId` and `currentLocationId` with a default UUID so request headers are always present.

---

## Fixtures

Fixtures create the prerequisite data a test needs via the real API. They exist to remove setup noise from feature files.

A simple fixture call:
```kotlin
val categoryId = categoryFixtureBuilder.create()          // random defaults
val categoryId = categoryFixtureBuilder.create(           // specific values
  ProductCategoryInsertDto(categoryName = "Electronics")
)
```

Fixtures compose — `ProductFixtureBuilder` internally builds a product group (which builds a category) and a base unit, then hands back the two IDs a product creation needs:
```kotlin
val fixture = productFixtureBuilder.create()
// fixture.productGroupId, fixture.baseUnitId — ready to use
```

When a step needs a product to exist, it calls the fixture and stamps those IDs onto every row:
```gherkin
Given the following products exist:
  | productName | description  |
  | Widget A    | First widget |
  | Widget B    | Second widget|
```

The step converts each row to `OrganizationProductInsertDto` via `DtoConverter`, then `.copy(productGroupId = ..., baseUnitId = ...)` from the fixture. The feature file only describes business data — never IDs.

---

## Cross-Step Data (`InjectContext`)

When a step creates something, it stores the result so later steps can reference it using `#key->index` (0-based).

```gherkin
When I create a product with name "Widget" and description "test"
Then the response status should be 200
And I store the response "id" as "product"

When I send a GET request to "/secured/products/#product->0"
Then the response field "productName" should be "Widget"
```

Creating multiple things under the same key and accessing by index:
```gherkin
Given the following products exist:
  | productName | description |
  | Widget A    | first       |
  | Widget B    | second      |

# #product->0 is Widget A, #product->1 is Widget B
When I send a DELETE request to "/secured/products/#product->1"
```

`InjectContext` is cleared before every scenario. Steps store using typed key constants (e.g. `ProductContext.ID`, `LocationContext.ID`) so there are no magic strings in Kotlin code — only in feature files where that is intentional.

---

## DataTable to DTO

Column names must match DTO field names exactly. `DtoConverter` handles the conversion and resolves any `#key->index` placeholders in cell values before Jackson parses them.

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

## Scenario Lifecycle

Before every scenario:
1. All test tables are truncated (`RESTART IDENTITY CASCADE`) — platform schema and Liquibase tables excluded
2. Auth, response, and inject contexts are reset

For `@kafka-consumer` scenarios, before the scenario also:
1. Resets `catalog-sync-group` offsets to latest (skips any backlog from previous runs)
2. Ensures the public schema org exists
3. Starts the application's Kafka listener containers

After `@kafka-consumer` scenarios, listeners are stopped so they don't bleed into the next scenario.

Kafka polling timeouts: 15 s for published events, 20 s for consumer sync effects.
