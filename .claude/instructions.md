About
------
This file contains instructions for Claude, an AI language model developed by Anthropic. The instructions guide Claude's behavior and responses during interactions.

Project Requirements:
----------------------
- Retail Software Solution (RTSS) is a comprehensive retail management software designed to streamline operations for small to medium-sized retail businesses.
- The overall goal is to create a user-friendly, efficient, and scalable solution that integrates various retail functions into a single platform.
- The software should be modular, allowing organizations to customize features based on their specific needs.
- The solution must support multi-location management, inventory tracking, sales reporting, customer relationship management, etc.

Database Design:
----------------
The database is structured to support multi-tenant architecture, where each organization can have multiple locations.
Each organization has its own schema, namely org-a, org-b, etc.
Each location equally has its own schema such as loc-1, loc-2, etc.
The location table within each organization schema maintains the schema names for its locations.

Liquibase is used for database versioning and migrations to ensure smooth updates and consistency across different environments.
The files are location under @resources/db/changelogs.
The order of execution is
	@resources/db/changelogs/platform/
	1. tables/*
	2. version-entries/*
	3. registry-entries/*

- PostgreSQL is the chosen database system.
- Admin actions don't require heavy transactional support, so the default isolation level of Read Committed is used.

### Table Registry
- Insert all new tables in `registry-entries`
- Register audit tables in `TableNames` and registry as well
- Prefix limited to 5 characters

### Changeset Management (pre-live)
- Reuse existing changesets for table modifications
- Each changeset gets a version label. For now, all labels are 1.0.0
- Simply delete unwanted changesets when dropping indexes, constraints, etc. No need to add a drop changeset.

### New Table Creation
- Add sequence for reference number column
- Use appropriate sequence to get next reference number
- reference number has a prefix defined in the registry. Append the next number to it to fill at least 6 characters total
- For example, if prefix is "OR-" and next number is 42, reference number is "OR000042"
- Create sequences, triggers, extensions, FKs, indexes in separate folders
- Add entity, repository, service, controller classes as needed

### Mapping
- Use MapStruct for mapping between entities and DTOs
- For every entity, there will be a corresponding DTO that abstracts away database details
- This dto is not necessarily the same as the API request/response objects, which can be defined separately
- Use `@Context` parameters in mappers to pass additional information needed for mapping. 
- Context will avoid `O(n)` queries in mappers

### Entity & JPA Rules
- No migration info in entities — only column names + insert/update restrictions
- Use `TableNames` constants for table names
- Add auditing columns (`created_by_id`, `created_on`) on all tables
- Avoid `@Enumerated`; use converter classes for enums with column sizes defaulted to 5 characters.

### Naming & Formatting
- Constraint pattern: `tblname_colname_constrainttype`
- Blank lines between changesets and between column definitions
- variable and method names should be verbose. For example,
  - `val ceilingService: PurchasePaymentCeilingService` is bad
  - `val purchasePaymentCeilingService: PurchasePaymentCeilingService` is good

### Comments
- Gotchas only — never restate what the code already says.
- A comment describes the code as it now stands, never the change that produced it. It is read by
  someone who has no idea what the previous version looked like.
- Banned: what the code used to do, what the old approach was, how many call sites a migration
  touched, ticket/PR references, and words that date the comment (`now`, `previously`, `used to`,
  `no longer`, `recently`, `this change`). That narrative belongs in the commit message.
- Naming a rejected alternative is allowed only to stop someone reverting to it, and only stated
  as a durable property of that alternative — `@CreationTimestamp applies at INSERT, so the field
  is null after save()`, not `we switched off @CreationTimestamp because ~20 call sites needed
  saveAndFlush`.
- State a rationale once, at the most relevant spot. Don't repeat the same "why" at every call
  site; if it spans a package, it goes in that package's README instead.
- Don't defend against an edit the compiler or type system already prevents (a reordering blocked
  by a data dependency, an elvis on a non-nullable type).
- Test comments: name the invariant being guarded and what breaks if it regresses — not the bug
  or task that prompted writing the test.

### Auditing
- Add YAML for audit table + expected indexes
- Register audit table in `TableNames` and registry
- Create an index on `rev` and `(id, rev)` for audit tables
- We don't audit lookup tables, and unachangeable columns like created_on, created_by_id, reference_number

Project Structure:
-------------------
The backend system is organized into the following main components:
	- platform
	- organizations
	- locations

- The Platform
	Manages cross-organization metadata and shared resources such as
		- The table registry
		- Organization details
		- Database Versioning and Migrations
		- User Management

- Organizations
	Handles organization-specific data and configurations, including
		- products
		- categories
		- locations
		- organization users
		- payment methods
	When making LocalDate related logic, always consider the `me.ezra_home.retail_software_solution.util.business.DateTimes` class

- Locations
	Focuses on location-specific operations and data, such as
		- inventory management
		- sales transactions

Some packages have README like @me/ezra_home/retail_software_solution/locations/business/sale/README.md
Use them when scanning in their vicinity. When we make changes, the relevant README should be updated 
to reflect the current state of the code and any important details about the implementation.
This is not a change log, but a current state document.

Separation of Concerns:
------------------------
- Each domain layer is likely to have its own set of entities, repositories, services to manage its specific data and business logic. 
- The modular design allows for clear separation of concerns and easier maintenance.
- There will be an api package at each domain layer to handle communication with other areas of the system.
- `ArchitectureTest` will be used to enforce architectural boundaries and ensure that dependencies flow in the intended direction.
- Avoid importing from other domain layers except for shared modules like utils, exceptions, etc. 
- When cross-layer communication is necessary, use classes from the api package of the external domain.
- For example, if a list of accounts is needed, use the account service instead of the account cache/repository directly.
- Avoid extension functions when a regular method would suffice.

The piece below could change in some cases, but here is the most common flow:
Insert Flow:
--------------
- InsertDto will be mapped straight to entity.
- The entity will be saved and its result immediately converted to DomainDto.
- This DomainDto is what will be released to the service layer and rest of codebase. It will have correct types, no unnecessary nullables.

Update Flow
-------------
- UpdateDto should have an applyTo(existing: XxxDomainDto): XxxDomainDto method that handles the merge.
- Only updatable fields are mentioned
- Other than the id, most fields show be Optional<TYPE>?. 
- The nullability allows us to distinguish between "not provided" and "explicitly set to null/empty".
- We have a generic OptionalQualifier to help, but applyTo can handle it on its own.
- generated fields(id, referenceNumber, createdOn, createdById) are carried through automatically by copy().
- Do not use @MappingTarget mappers for partial updates.

----------------
- When responding to queries about the project, ensure that answers are tailored to the context of retail management software.
- Provide explanations that are clear and concise, avoiding unnecessary technical jargon unless specifically requested.
- Emphasize the modularity and scalability of the RTSS solution in responses.
- Use utils when available like StringUtils, DateTimes, etc
- Avoid inline FQNs by using imports and constants everywhere. Do ask me if you ever want to use a FQDN inline

KAFKA
----------------
- We ensure idempotency by doing `TransactionEventProcessor.shouldProcess`. 
- For concurrency safety, we need db constraints on entities that can be created by kafka.
- These are examples of such constraints:
  - `add-unique-constraint-tax-entry-source-ref-type-tax-type` for `SaleTaxReversalProcessor` and `SaleTaxFinalizationProcessor`
  - `add-unique-constraint-stock-movement-ext-ref-type-product` for `PurchaseDeliveryStockUpdater`
- When adding processors, reason about this idempotency surface and add constraints as needed.
- If the processor creates a new entity, there should be a unique constraint on the natural key of that entity to prevent duplicates during retries.
