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

Reports land in `build/reports/cucumber.html` and `build/reports/cucumber.json`.

---

## Lanes and Tags

Each lane is a Gradle task that filters by tag. Tag scenarios to control which lanes they appear in.

| Lane             | Gradle Task                 | Run when                                                           |
|------------------|-----------------------------|--------------------------------------------------------------------|
| `smoke`          | `cucumberSmokeTest`         | `@smoke and not @ignore`                                           |
| `kafka-producer` | `cucumberKafkaProducerTest` | `@publishes-to-kafka and not @consumes-from-kafka and not @ignore` |
| `kafka-consumer` | `cucumberKafkaConsumerTest` | `@consumes-from-kafka and not @ignore`                             |
| `regression`     | `cucumberRegressionTest`    | `not @ignore`                                                      |

**`@publishes-to-kafka`** — asserts that an action publishes the right event to the topic. Before the scenario a Kafka consumer subscribes from latest; the step polls and asserts message shape. The application's own listeners are not running.

**`@consumes-from-kafka`** — tests the full event pipeline. The application's listeners are started before the scenario, consumer offsets are reset to latest, and the test asserts the downstream effect (e.g. a product was synced to the location catalog). Slower and heavier than publish tests.

Other tags: `@ignore` to skip everywhere.

---

## Boilerplate Session

`BoilerPlateDataInitializer` runs once after Spring starts. It bootstraps a complete org/location pair through the real API and stores both IDs in `InjectContext` as persistent keys.

If the organization already exists (container reuse across runs), it rehydrates from the cache instead of recreating.

Every scenario starts with a real organization and location already in place. Reference them with `#organization` and `#location`.

---

## Authentication

The default token before every scenario is `PLATFORM_ADMIN` (set by `TestHooks`). Steps override it:

```gherkin
Given I am authenticated as an organization user    # org-level access, no platform roles
Given I am not authenticated                        # clears token, expects 403 on secured endpoints
```

No real Okta involved — `TestAuthenticationFilter` maps tokens to principals and roles directly.

---

## Fixtures

Fixture steps create prerequisite data through the real API and store the resulting ID in `InjectContext`. Steps must appear in dependency order.

```gherkin
Given a category exists
Given a product group exists      # requires: category
Given a unit group exists
Given a unit exists               # requires: unit group
```

Products can be created individually or in bulk:

```gherkin
When I create a product with name "Widget" and description "A widget"

Given the following products exist:
  | productName | description  |
  | Widget A    | First widget |
  | Widget B    | Second widget|
```

Both resolve `PRODUCT_GROUP` and `UNIT_VALUE` from `InjectContext`. Feature files describe business data only — never IDs.

---

## Cross-Step Data (`InjectContext`)

`InjectContext` has two stores:

- **Transient** (`TransientKey`) — cleared before every scenario
- **Persistent** (`PersistentKey`) — survives for the entire test session

When a step creates something, it stores the resulting ID under its key. Later steps reference it with `#keyname` (last added) or `#keyname->index` (0-based).

**TransientKey names:** `product`, `category`, `productGroup`, `unitGroup`, `unitValue`

**PersistentKey names:** `organization`, `location`

```gherkin
Given the following products exist:
  | productName | description |
  | Widget A    | first       |
  | Widget B    | second      |

When I DELETE from /secured/products/#product->1
```

`AuthenticatedRequestFactory` automatically attaches `X-Organization-Id` and `X-Location-Id` headers from `InjectContext` on every request — no manual header setup needed.

---

## DataTable to DTO

Column names must match DTO field names exactly. `DtoConverter` resolves `#key` and `#key->index` placeholders before Jackson parses.

Special cell value: `NULL` → `null`

```gherkin
Given the following products exist:
  | productName | description | tagsToAdd |
  | Widget      | A widget    | NULL      |
```

---

## Request / Response Steps

No quotes around paths; `#injectionKey` and `<exampleVar>` resolve before the request fires. `lastError` is populated automatically when status ≥ 400.

### Requests

```gherkin
When I GET from secured/products
When I GET from secured/products/#productId
When I POST to secured/products with payload:
  """
  { "productName": "Widget", "productGroupId": "#productGroup" }
  """
When I GET from secured/products with query parameters:
  | active | true |
  | limit  | 10   |
When I DELETE from secured/products/#productId
```

Pick `to` or `from` to read naturally for the verb — they're equivalent. Replace `query` with `matrix` for matrix parameters. Supported methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

### Responses

```gherkin
Then response returns with status 200
Then response returns with status 400 with message: Product name already in use
Then response list size is 3
Then response contains details:
  """
  { "productName": "Widget", "active": true }
  """
Then response contains item with details:
  """
  { "productName": "Widget" }
  """
Then response contains no item with details:
  """
  { "productName": "Deleted" }
  """
Then response contains details with exact lists in order:
  """
  { "lines": [ {"productId": "#product->0"}, {"productId": "#product->1"} ] }
  """
```

- Default match is **subset**: extra fields/items in the actual response are ignored.
- `with exact lists` requires identical sizes; add `in order` to require positional match.
- JSON values wrapped in `^...$` are treated as regex.
- Numbers compare as `BigDecimal` floored to scale 7 (so `100` matches `100.00`).
- Failure messages include the JSON path of the mismatch (`at $.lines[2].quantity: expected "5" but got "3"`).

Domain-specific steps live in `steps/organizations`, `steps/locations`, and `steps/platform`. Add them only when behavior cannot be expressed via generic grammar (e.g. multi-call orchestration).

---

## Database Verification

One step file (`DatabaseSteps.kt`) covers every entity. **No per-entity step file is needed.** `DataAccessHelper` auto-registers every `JpaRepository` bean at boot, deriving the lookup key from the entity class.

Entity names in features use snake_case with capitalized words (`Organization_Product`, `Sale_Line`). Lookup ignores case and underscores so `OrganizationProduct` resolves to the same package.

```gherkin
Then Organization_Product should exist in database with id #productId
Then Organization_Product should not exist in database with id #productId
Then Organization_Product should exist in database with id #productId and options:
  """
  { "productName": "Widget", "isActive": true }
  """
Then Sale should match example:
  """
  { "totalAmount": "100.00", "status": "CONFIRMED" }
  """
Then Subledger_Entry table should have exactly 3 records in database
Then Subledger_Entry table should have no records in database
```

Matching uses the same `JsonSubsetMatcher` as REST — same regex, numeric tolerance, subset semantics, and path-aware failure messages.

Only simple IDs are supported (UUID, Long, Int, String). Composite IDs not yet supported.

For repositories whose auto-derived key is ambiguous or verbose, register an alias:

```kotlin
dataAccessHelper.alias("Product", OrganizationProductRepository::class)
```

---

## Kafka Steps

**`@publishes-to-kafka`** — poll the topic for up to 15 s, store the matched event in `KafkaContext`, then assert its `entityId` matches the response `id`.

**`@consumes-from-kafka`** — poll the downstream read endpoint for up to 20 s for async consumption to complete.

---

## Scenario Lifecycle

Before every scenario:
1. All test tables truncated (`RESTART IDENTITY CASCADE`) — Liquibase tables, `organization`, `location`, `authorization_pass`, `reserved_subdomain`, and `table_registry` are excluded
2. Transient `InjectContext`, `ResponseContext`, and `KafkaContext` reset
3. Organization/location-scoped caches cleared
4. `AuthContext` initialized to `PLATFORM_ADMIN`

For `@publishes-to-kafka` — before: subscribes a consumer to `CATALOG_EVENTS` from latest offset. After: closes that consumer.

For `@consumes-from-kafka` — before: resets `catalog-sync-group` offsets to latest, starts Spring Kafka listeners. After: stops listeners so they don't bleed into the next scenario.
