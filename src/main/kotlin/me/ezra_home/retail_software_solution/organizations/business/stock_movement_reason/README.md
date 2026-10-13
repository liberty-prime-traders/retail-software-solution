# Stock movement reasons

Org-level lookup of reasons a stock movement may carry (`stock_movement.reason_id`, nullable,
no database constraint: the movement lives in a location schema, the reason in the org schema).
All rules live in the service layer.

## Reasons and movement types

Each reason holds the `MovementType`s it applies to in `movement_types`, stored the way
`ContactEntity.contactTypes` is (`MovementTypeSetConverter`, comma-separated codes). A type that
no reason lists rejects every non-null reason; there is no per-type special case.

System reasons come from `StockMovementReason` (`systemDefined = true`), each with
`allowedMovementTypes`. They are fully locked: `StockMovementReasonService.update` rejects any
change, including `movementTypes`, and `delete` rejects them too. Deleting an unknown id is a no-op.

Nothing stops deleting an org reason that stock movements still reference (they live in location
schemas); the movement history then shows no reason name for them.

REST: `StockMovementReasonEndpoint` at `secured/stock-movement-reasons` (get all, create,
update, delete by id).

Org-created reasons must list at least one movement type (`StockMovementReasonValidator`) and
may list any.

## Required versus optional

`MovementType.reasonRequired` is the single definition: `STOCK_ADJUSTMENT_INCREASE` and
`STOCK_ADJUSTMENT_DECREASE` require a reason; every other type accepts none.

## Recording a movement

`StockMovementReasonService.guardReason(movementType, reasonId)` is the check a movement writer
calls before persisting. A null reason is rejected only for required types. A non-null reason
must exist and list the movement type. No date check. No existing updater passes a reason yet;
the adjustment and return updaters are the intended callers.

## Seeding

`StockMovementReasonSeeder` inserts missing system reasons and, on every run, syncs
`movementTypes` of existing system rows to the enum, so a changed `allowedMovementTypes` applies
on the next seed. It writes through the cache and bypasses the service guards on purpose.
