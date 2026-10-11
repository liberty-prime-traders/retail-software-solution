# contact_opening_balance

Lets org admins declare a contact's starting receivable or payable balance
(`ContactSubledgerDirection.RECEIVABLE` = they owe us, `PAYABLE` = we owe them). It
mirrors `opening_balance` (see its README): rows are **append-only**, a zero
delta records nothing, negative amounts are rejected, and the ledger post is
async through a Kafka event.

## Revisions

`contact_opening_balance` holds one row per revision; the latest row by
`created_on` per contact and subledger direction is the current
figure. Earlier rows are the audit trail. The two directions of one contact
are revised independently.

`ContactOpeningBalanceService.set`:

1. Rejects a negative amount, an inactive contact
   (`ContactStatus` other than `ACTIVE`), and a closed fiscal period for today.
2. Takes `OrgEntityAdvisoryLock` on `LockNamespaces.CONTACT_OPENING_BALANCE`
   per contact reference number. Rows are append-only, so a `FOR UPDATE` on the
   latest row cannot serialize two revisions (both read the same latest row and
   insert siblings), and a first revision has no row to lock.
   This namespace is separate from `SUBLEDGER_CONTACT`, which the ledger post
   takes later in its own transaction.
3. The amount is rounded to scale 4 (`Decimals.roundToScale4`) before it is compared or stored, so the
   delta is measured between two stored-scale values. `signedDelta = roundedAmount - latest.amount`.
   Zero returns without a row or event.
4. Inserts the revision and publishes `ContactOpeningBalanceUpsertedEvent`
   through `ContactOpeningBalanceHandlerForKafka.publish`, bound to the commit.

The ledger post is dated today in the organization timezone (as for account
opening balances), and the fiscal period check is against today.

## Posting (`ledger/processors/ContactOpeningBalanceAccountingProcessor`)

| Subledger | Raising (delta > 0) | Reducing (delta < 0) |
|---|---|---|
| RECEIVABLE | debit `TRADE_RECEIVABLES`, credit `OPENING_BALANCE_EQUITY` | sides swapped |
| PAYABLE | debit `OPENING_BALANCE_EQUITY`, credit `TRADE_PAYABLES` | sides swapped |

The posted amount is `abs(delta)`; one routine picks the sides from the sign,
there is no separate correction path. `TRADE_RECEIVABLES` / `TRADE_PAYABLES`
are the control accounts `OpeningBalanceAccountValidator` bars from manual
account opening balances, so these postings are the only way their totals
originate.

The event carries the positive `amount` and a `balanceReduced` flag (derived from the signed delta in
`ContactOpeningBalanceHandlerForKafka.publish`). The `SubledgerEntryRequest` follows the ledger's
column convention (`ledger/README.md`, "Subledger columns"): the amount goes in `receivableAmount`
when the contact ends up owing us more (receivable raised, payable reduced) and in `payableAmount`
otherwise, so `subledger_entry` never holds a negative amount and `subledger_balance` is patched by
`LedgerPostingService` as for any posting.
The entry's type is its `ledger_entry_group`: `LedgerSourceType.CONTACT_OPENING_BALANCE`,
whose `sourceReferenceNumber` is the revision's `COB` reference number; that
reference is also the idempotency key (`LedgerPostingGate.isPosted`).

## Reissue

`ContactOpeningBalanceHandlerForKafka.reissue(sourceDocumentId)` loads that
row, finds the previous row of the same contact and subledger direction by `created_on`, and
republishes `amount - previous.amount` dated at the revision's creation day,
so a late DLT retry reproduces the original delta.

## REST surface

`ContactOpeningBalanceEndpoint` (`secured/contacts/{contactId}/opening-balances`,
`Feature.CHART_OF_ACCOUNTS`): `POST` (body: `ContactOpeningBalanceSetDto` —
`subledgerDirection`, `amount`) → `set`. There is no history endpoint.

## Reading: `ContactOpeningBalanceReader`

`ContactService` adds `netOpeningBalance` and `netOpeningBalanceSignal` to
`ContactResponseDto`, the way accounts carry their own opening balance. The
reader returns the latest revision per subledger as a `SubledgerBalanceDto`
(receivable and payable opening amounts), so the net is `abs(receivable - payable)`
and the signal follows the same `BalanceSignal` rules as the contact's running
balance; a contact with no revisions reads as `SubledgerBalanceDto.ZERO`. It is a
bean separate from `ContactOpeningBalanceService`, which depends on
`ContactService`, to avoid a bean cycle.

The ledger tab (`POST secured/contacts/{id}/ledger`) shows the revisions as rows
whose `sourceType` is `CONTACT_OPENING_BALANCE`.
