# contact_opening_balance

Lets org admins declare a contact's starting receivable or payable balance
(`ContactSubledgerDirection.RECEIVABLE` = they owe us, `PAYABLE` = we owe them). It
mirrors `opening_balance` (see its README): rows are **append-only**, a zero
delta records nothing, negative amounts are rejected, and the ledger post is
synchronous, in the saving transaction.

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
   A side whose delta is zero writes no row and posts nothing, so a call may write one row, two, or none.
4. Inserts the revision and posts it with `LedgerPostingService.post` in the same transaction
   (`ContactOpeningBalancePostingRequests.revised`). A posting failure, such as a closed fiscal period, rolls the
   revision back and reaches the caller.

The ledger post is dated today in the organization timezone (as for account
opening balances), and the fiscal period check is against today.

## Posting (`ContactOpeningBalancePostingRequests`)

| Subledger | Raising (delta > 0) | Reducing (delta < 0) |
|---|---|---|
| RECEIVABLE | debit `TRADE_RECEIVABLES`, credit `OPENING_BALANCE_EQUITY` | sides swapped |
| PAYABLE | debit `OPENING_BALANCE_EQUITY`, credit `TRADE_PAYABLES` | sides swapped |

The posted amount is `abs(delta)`; one routine picks the sides from the sign,
there is no separate correction path. `TRADE_RECEIVABLES` / `TRADE_PAYABLES`
are the control accounts `OpeningBalanceAccountValidator` bars from manual
account opening balances, so these postings are the only way their totals
originate.

The request carries the positive `abs(delta)`; the sign of the delta picks the sides and the subledger column. The
`SubledgerEntryRequest` follows the ledger's
column convention (`ledger/README.md`, "Subledger columns"): the amount goes in `receivableAmount`
when the contact ends up owing us more (receivable raised, payable reduced) and in `payableAmount`
otherwise, so `subledger_entry` never holds a negative amount and `subledger_balance` is patched by
`LedgerPostingService` as for any posting.
The entry's type is its `ledger_entry_group`: `LedgerSourceType.CONTACT_OPENING_BALANCE`,
whose `sourceReferenceNumber` is the revision's `COB` reference number.

## Settlements (`contact_opening_settlement`)

A pre-live debt is paid down without a sale or purchase document: the contact's declared opening balance is the
document, and a settlement is a payment against it. `RECEIVABLE` records money received from the contact (their
pre-live sale balance), `PAYABLE` records money paid to the contact (our pre-live purchase balance). Each reduces what
is outstanding on its side and moves the contact's net toward zero.

- `contact_opening_settlement` (`CSET`) and `contact_opening_settlement_void` (`CSETV`) are immutable and append-only.
  A void is unique per settlement, carries `void_date` (always today in the organization zone) and an optional reason;
  corrections are void and re-enter. A settlement is **active** when it has no void row; that rule lives only in the
  `active_contact_opening_settlement` view (`ActiveContactOpeningSettlementEntity`), summed by
  `ContactOpeningSettlementFinder.activeSettledAmount`.
- The user picks a payment method; `PaymentMethodDataFetcher.findAccountCode` resolves it and the account code is stored on
  the row (FK to `account`), so a void reads the original account, never the method's current one.
- `outstanding` = latest declared opening amount for that direction − active settlements
  (`ContactOpeningSettlementFinder.outstandingAmount`). A settlement above it is rejected, so a contact with no declared
  opening balance cannot be settled: declare it first. The date cannot be in the future and the contact must be `ACTIVE`
  (a void has no contact check).
- `ContactOpeningBalanceService.set` rejects a target for a direction below its active settled total, so `outstanding`
  never goes negative; void the settlements first. `ContactOpeningBalanceReader` and `netOpeningBalance` still report the
  declared figure, not the outstanding one.
- Recording and voiding take `OrgEntityAdvisoryLock` on `LockNamespaces.CONTACT_OPENING_BALANCE` per contact reference,
  the same lock `set` takes, before reading the outstanding amount. The row is flushed (`saveAndFlush`) before the
  response is built because the view does not see pending writes.
- Posting is synchronous, in the saving transaction (`LedgerPostingService.post`), as it is for the revisions above.
  `ContactOpeningSettlementPostingRequests` builds both requests; postings are listed in `ledger/README.md`. The
  open fiscal period and account postability are enforced by `LedgerPostingService.post`.
- `ContactOpeningSettlementEndpoint` (`secured/contact-opening-settlements`, `Feature.CHART_OF_ACCOUNTS`):
  `POST` (`ContactOpeningSettlementCreateRequest`), `POST void` (`ContactOpeningSettlementVoidRequest`) and
  `GET ?contactReferenceNumber=` all return `ContactOpeningSettlementSummaryResponse`: `receivableOutstanding`,
  `payableOutstanding` and every settlement newest first, each with its own `void`.

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
