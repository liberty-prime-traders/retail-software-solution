# Stock reservation

A draft sale holds stock for its lines without consuming it: one reservation per sale line, in storage units
(`StockReservationEntity`, table `sale_line_stock_reservation`, `storage_quantity_reserved`). Reservations are per product, not per
layer, and a draw never reads them; only the availability guard does.

- `StockReserver` (`api/`) is the only way in: `reserve(NewStockReservation)` writes reservations, `clearBySale` and
  `clearBySaleLineIds` remove them, and `loadReservationBreakdown` answers how much of each product is reserved, in total and per
  sale (`ProductReservations.total`, `forSale`, `excludingSale`).
- The domain depends on nothing in `stock`. The availability guard (`StockAvailabilityValidator`) and fetcher
  (`StockAvailabilityFetcher`) live in `stock/api/availability` because they combine the balance with the reserved total; they call
  into this domain, never the other way round. Keep it that way: a call from here into `stock` would close a cycle, since `stock`
  (the adjustment updater) already guards through the validator.
- The sale domain owns when reservations are issued and cleared (see `sale/README.md`, "Stock: Reservations vs FIFO Consumption").
