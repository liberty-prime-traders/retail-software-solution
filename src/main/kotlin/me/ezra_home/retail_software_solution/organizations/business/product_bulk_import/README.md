# product_bulk_import

Lets a caller submit a whole payload of product categories, product groups,
and products in one request (`POST secured/products/bulk`) instead of
building them up one at a time through `product_category`'s, `product_group`'s,
and `product`'s single-item endpoints. This domain only ever imports from
those three domains' (plus `unitvalue`'s) `api/` packages — it owns no
entities of its own.

## Two phases: validate everything, then save everything

`BulkProductImportService.bulkImport` runs `BulkProductImportValidator.validate`
first, which returns a `List<String>` of every problem in the payload
(missing names, within-payload duplicates, a `categoryName`/`groupName`/
`unitCode` that resolves to nothing) instead of throwing on the first one.
If that list is non-empty, `bulkImport` throws
`RtsGenericException("Bulk product import validation failed", errors)`
before anything is saved. Only once validation is fully clean does phase
two run: categories, then product groups, then products, in that order,
since each depends on the previous step's saved ids. Categories and groups
that already exist (by `StringUtils.isEquivalent` name match) are reused
rather than recreated; products are never reused — a duplicate product name
reaching phase two means phase one missed it.

## Why bulk save paths exist instead of looping single-item creates

`ProductCategoryService.createCategory`, `ProductGroupService.createProductGroup`,
and `OrganizationProductService.createProduct` each evict their cache
(`ProductCategoryCache`/`ProductGroupCache`/`OrganizationProductCache`,
`@CacheEvict(allEntries = true)`) on every call. Looping any of them for N
rows would mean N full-cache evictions for one import. This is the exact
problem `unit_bulk_import` already solved for unit groups/values, and this
domain follows the same convention rather than inventing a different one:

- `ProductCategoryService.bulkCreateValidatedList`,
  `ProductGroupService.bulkCreateValidatedList`, and
  `OrganizationProductService.bulkCreateValidatedList` map straight to
  entities (no per-item validation — the whole batch was already validated
  in phase one) and persist through `ProductCategoryCache.saveAll` /
  `ProductGroupCache.saveAll` / `OrganizationProductCache.saveAll`, each a
  single `@CacheEvict`.
- The `ValidatedList` suffix is deliberate, matching `UnitGroupService`/
  `UnitValueService` — it's the caller's contract that the list has already
  been validated, so the method body can skip straight to persistence. See
  `.claude/instructions.md`'s Comments section for why this is a naming
  convention, not a comment above the method.
- `OrganizationProductService.bulkCreateValidatedList` still publishes one
  `CatalogEventHandler.publish(TableName.PRODUCT, id)` per created product
  (Kafka doesn't have a "cache" to thrash — one event per row is the normal,
  expected volume, same as the single-item path) but skips
  `ProductTagService.manageProductTags`, since bulk-imported products carry
  no tags (`ProductBulkInsertDto` has no tags field).
- `BulkProductImportService` builds the id-resolution maps
  (`categoryName -> id`, `groupName -> id`, `unitCode -> id`) from one
  up-front read per entity type, filters out rows that already resolve to
  an existing category/group, and passes only the genuinely new rows to
  each `bulkCreateValidatedList` call — one call, and one cache eviction,
  per entity type per import, regardless of row count.

## REST surface

`ProductEndpoint` (`secured/products`):

- `POST bulk` (body: `BulkProductImportRequestDto` — `categories`,
  `productGroups`, `products`) → `BulkProductImportService.bulkImport`,
  returning `204 No Content`. Callers use the existing fetch endpoints to
  retrieve what was saved.

Thin delegator; all validation/save-ordering/cache logic lives in
`BulkProductImportService` and `BulkProductImportValidator`.
