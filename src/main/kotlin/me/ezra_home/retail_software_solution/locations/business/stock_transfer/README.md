# stock_transfer

Handles the full lifecycle of inter-location stock transfers: draft → dispatched → received.

## Schemas involved

A stock transfer spans **two** location schemas and **one** org schema:

| Schema | What lives there |
|---|---|
| Source location schema | `stock_transfer_dispatch`, `stock_transfer_dispatch_line`, `stock_transfer_draft_line`, stock entries, stock movements |
| Destination location schema | `stock_transfer_receipt`, `stock_transfer_receipt_line`, destination stock entries and movements |
| Org schema | `stock_transfer_order` — the canonical status record visible to both locations |

Because each schema requires its own connection, every cross-schema read or write uses `withLocationSchema(schema) { ... }` to switch the search path, which requires `REQUIRES_NEW` (a fresh connection) on the gateway method called inside.

## Gateway pattern

`StockTransferSchemaGateway` is a pure data access component for the source location schema. It must not publish Kafka events — event publishing belongs in the service layer, where transaction scope is controlled. This is the rule that prevents the Kafka event from firing before the org-schema write completes.

## Event sequencing rule

All three events (`StockTransferDispatchedEvent`, `StockTransferCancelledEvent`, `StockTransferReceiptCompletedEvent`) must be published **after** the corresponding org-schema status update, within the outer service transaction. They fire on `AFTER_COMMIT` of that transaction. If they were published inside a `REQUIRES_NEW` gateway call, they would fire before the org write — leaving the two schemas inconsistent.

## FIFO allocation

`StockTransferFifoAllocator` splits a draft line into one or more dispatch lines, one per cost group. A single draft line for a product with stock at two different unit costs produces two dispatch lines. This means `recordTransferReceipt` in `StockTransferReceiptStockUpdater` must accumulate a running balance per `locationProductId` across lines — not use a single pre-fetched balance for each line — because multiple lines for the same product share the same balance accumulator.

## Destination location product resolution

Dispatch lines store the **source** location's `locationProductId`. When writing to the destination schema (receipt lines, stock entries, stock movements), the destination `locationProductId` must be looked up by `productId` via `LocationProductDataFetcher.findIdentityByProductId`. Never copy the source `locationProductId` to the destination.

## Kafka processor conventions

Both `StockTransferDispatchedInventoryProcessor` and `StockTransferCancelledInventoryProcessor` implement `EventReissueHandler`. The `sourceDocumentId` convention for both is the **transfer order ID** (org schema), matching the convention of their corresponding handler beans. The session context must be set to the correct location schema (source for cancelled, destination for dispatched) before `reissue` is called.

## Locking

`StockTransferReceiptService.confirmLine` and `unconfirmLine` both acquire `LockNamespaces.STOCK_TRANSFER_ORDER` before touching receipt lines. Dispatch (when LG-4 is addressed) requires both `LockNamespaces.PRODUCT` on each draft line's product (to serialize with concurrent sales) and `LockNamespaces.STOCK_TRANSFER_ORDER` on the order (to serialize concurrent dispatch attempts).
