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

1. Rejects a negative target payable or receivable (neither side is written), an inactive contact
   (`ContactStatus` other than `ACTIVE`), and a closed fiscal period for today.
2. Takes `OrgEntityAdvisoryLock` on `LockNamespaces.CONTACT_OPENING_BALANCE`
   per contact reference number, once for both sides. Rows are append-only, so a `FOR UPDATE` on the
   latest row cannot serialize two revisions (both read the same latest row and
   insert siblings), and a first revision has no row to lock.
   This namespace is separate from `SUBLEDGER_CONTACT`, which the ledger post
   takes later in its own transaction.
3. `ContactOpeningBalanceSetDto` is partial: `payable` / `receivable` are `Optional<Money>?`, where null
   leaves that side at its latest row, `Optional.empty()` clears it to zero and `Optional.of(x)` sets it
   (`applyTo(existing)` yields the target balance). Each side is revised against its own latest row. An amount with more decimals than the money scale is rejected when the request is read, so the
   delta is measured between two whole-cent values. `signedDelta = targetAmount - latest.amount`.
   A side whose delta is zero writes no row and publishes no event, so a call may write one row, two, or none.
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

`ContactOpeningBalanceEndpoint` (`secured/contact-opening-balances`, `Feature.CHART_OF_ACCOUNTS`):

- `POST {contactId}` (body: `ContactOpeningBalanceSetDto` — `payable`, `receivable`, each optional) → `set`.
- `GET history?contactReferenceNumber=` → `getHistory`: every revision of that contact, both
  sides, oldest first, as `ContactOpeningBalanceRevisionDto`: the `subledgerDirection`, the claimed
  `amount` and its `balanceEffect`, with `changedAt` and `changedBy` (the creator's full name, empty when the user is
  gone). The effect is the claim read from the organization's side: a receivable is `ADDS`, a
  payable is `SUBTRACTS`, and a claim of zero is `ZERO`. The history records what was claimed, not
  its ledger effect; deltas and postings belong to the contact's subledger history
  (`POST secured/contacts/{id}/ledger`).

## Reading: `ContactOpeningBalanceReader`

`ContactService` adds `netOpeningBalance` and `netOpeningBalanceEffect` to
`ContactResponseDto`, the way accounts carry their own opening balance. The
reader returns the latest revision per subledger as a `SubledgerBalanceDto`
(receivable and payable opening amounts), so the net is `abs(receivable - payable)`
and the effect follows the same `BalanceEffect` rules as the contact's running
balance; a contact with no revisions reads as `SubledgerBalanceDto.ZERO`. It is a
bean separate from `ContactOpeningBalanceService`, which depends on
`ContactService`, to avoid a bean cycle.

The ledger tab (`POST secured/contacts/{id}/ledger`) shows the revisions as rows
whose `sourceType` is `CONTACT_OPENING_BALANCE`.
