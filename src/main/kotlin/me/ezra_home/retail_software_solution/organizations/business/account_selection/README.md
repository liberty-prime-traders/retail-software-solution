# Account selection

Which accounts a given picker offers and a given write accepts. One definition serves both, so the
selection tree and the validators cannot diverge.

## Rules (`api/AccountSelectionRule`)

One enum entry per use: `PAYMENT_METHOD`, `FUND_MOVEMENT`, `LOAN`, `TAX_PAYABLE`, `TAX_RECOVERABLE`, `EXPENSE_TYPE`.
`rejectionReason(candidate)` returns `null` when selectable, otherwise the message a validator throws;
`isSelectable` is that `== null`. Rules read an `AccountSelectionCandidate` (owned by `account/api`), never
`AccountDto`. They do not check "exists" or "active" — `AccountSelectionService.requireSelectable` and
`AccountTreeBuilder` do, before a rule runs.

Every rule offers leaf accounts only, because ledger postings are rejected on a parent account. That check lives in
`rejectionReason` itself; each entry adds only its type/placement requirement.

- `PAYMENT_METHOD` — `ASSET`; `CASH`, or a parent that is not system-maintained, or the `DIGITAL_PAYMENTS` parent.
- `FUND_MOVEMENT` — the same type and placement as `PAYMENT_METHOD` (both go through one shared check), without requiring a
  payment method row. Used for every account a user picks on a fund movement.
- `LOAN` — the same type and placement as `PAYMENT_METHOD`, without requiring a payment method row. Used for the asset
  account a user picks on a loan and on a loan repayment.
- `TAX_PAYABLE` / `TAX_RECOVERABLE` — a system-maintained account must sit directly under `TAX_PAYABLE` /
  `TAX_RECOVERABLE`; an org-defined one must be `LIABILITY` / `ASSET`.
- `EXPENSE_TYPE` — `EXPENSE` and not in the reserved set (`COST_OF_GOODS_SOLD`, `WAGES_EXPENSE`, `INBOUND_FREIGHT`,
  `OUTBOUND_FREIGHT`, `SHRINKAGE_AND_LOSSES`, `INTERNAL_USE_EXPENSE`, `BAD_DEBT_EXPENSE`, `TAX_EXPENSE`), which flows
  post to directly. Contra accounts never reach that check: the rule requires the exact `EXPENSE` type. The set is private to the rule; `expense_type/` depends on this domain,
  never the reverse.

## Callers

- Validators call `AccountSelectionService.requireSelectable(rule, accountCode)`: `PaymentAccountValidator`
  (`payment_method/`), `TaxAccountsValidator` (`org_jurisdiction_tax_type/`), `ExpenseTypeValidator`
  (`expense_type/`), `FundMovementAccountValidator` (`fund_movement/`), `LoanAccountValidator` (`loan/`). They keep only what is specific to their domain (blank code, recoverable-vs-not-recoverable
  tax types). `requireSelectable` reads committed state (`getFreshSelectionCandidates`), not the cache.
- `AccountSelectionService.buildTreesForSelection` builds `AccountsTreesForSelection` (`payable`, `recoverable`,
  `paymentMethods`, `expenseTypes`, `fundMovements`, `loans`) for `GET secured/accounts/selection-trees`. `AccountTreeBuilder` drops inactive
  accounts, then prunes any branch that is neither selectable nor an ancestor of a selectable node.

A new picker is a new rule here plus a validator that calls `requireSelectable` — never a predicate written
beside the validator.
