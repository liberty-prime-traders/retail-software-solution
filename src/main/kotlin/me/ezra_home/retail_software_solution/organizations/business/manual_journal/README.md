# manual_journal

A manually entered ledger journal in the organization schema. It posts balanced debit and credit lines to a fixed list of
chart accounts, and carries a summary, optional details, a posting date, and a reference number that becomes the ledger
group's `source_reference_number`.

It corrects the ledger only. It doesn't change stock, purchase, or delivery documents, so it isn't a fix for a delivery.
Stock quantities are still corrected with `stock_adjustment`.

## Tables

- `manual_journal`: header, `ImmutableEntity`. Reference number (prefix `MJ`), `summary` (required), `details` (optional),
  `posting_date`, and `reversal_of_manual_journal_id` (unique; set on the reversal). `summary` and `details` are unbounded `text`.
- `manual_journal_line`: the posted lines. Lines are listed sorted by account code. Has its own reference number (prefix `MJL`), and holds `account_code`, `entry_type`, `amount`, and
  `line_memo` (optional, unbounded `text`). Kept so a reversal can be built without reading the ledger back.

## Posting

`ManualJournalRecorder` posts through `LedgerPostingService.post` with source type `LedgerSourceType.MANUAL_JOURNAL`
(`MJ`) and the journal's own reference number. No subledger entries are posted; manual journals never touch a contact.

The header and lines are saved first, and the ledger post runs last. Both are in one transaction, so a ledger failure rolls the journal back.

Before the ledger is called, the service checks everything the ledger would reject, so the caller gets a message rather
than a ledger exception:

- Debits equal credits.
- The posting date is not in the future.
- The posting date falls in an open fiscal period.

## Accounts (`AccountSelectionRule.MANUAL_JOURNAL`)

Only these accounts can be used. Any other account is rejected, and the account picker offers only these, via the
`manualJournals` tree from the selection-trees endpoint.

- Fixed assets: Furniture & Fixtures, Equipment, Vehicles, Buildings, Land, Accumulated Depreciation.
- Equity: Owner's Capital, Owner's Draws, Opening Balance Equity.
- Expenses: Rent, Utilities, Office Supplies, Staff Welfare, Equipment & Electronics, Repairs & Maintenance, Other
  Operating Expenses, Shrinkage & Losses.
- Revenue: Other Income.

Cash, Trade Receivables, Trade Payables, Inventory, and the other system accounts are not on the list.
Accounts with contact tracking are not on it either, which is why contacts don't exist on a manual journal.

An account must also be active and a leaf (no child accounts), or it's rejected as not postable. The list is checked
under the account structure lock, so an account can't be deactivated between the check and the save.

## Rules (`ManualJournalValidator`)

- Summary is required.
- At least two lines.
- Every account is on the allowlist and is active and a leaf.
- Debits equal credits.
- A reversal can't be reversed, and an original can be reversed only once. A reversal needs a reason.

All failures come back together in one `errors` array (HTTP 400, see REST below).

## Reversal

`POST secured/manual-journals/{referenceNumber}/reversal` posts the stored lines with each entry type flipped, using
`reversal = true` so inactive accounts can be drawn down. The original journal is locked (`LockNamespaces.MANUAL_JOURNAL`) before the already-reversed check, so concurrent reversals of one journal serialize. The reversal is dated today, in the organization's zone, has
the request's `reason` as its summary, and gets its own reference number. It posts with source type
`MANUAL_JOURNAL_VOID` (`MJV`). It is dated today, so it needs an open fiscal period like a create. Account checks don't run on a reversal, since
the original was already checked when it was posted.

## REST

`ManualJournalEndpoint` (`secured/manual-journals`). Gated by `Feature.CHART_OF_ACCOUNTS`.

- `POST`: create. Requires `CREATE_MANUAL_JOURNAL`. Body: `summary`, `details?`, `postingDate?` (defaults to today in the
  organization's zone), `lines[]` (`accountCode`, `entryType` `DEBIT`/`CREDIT`, `amount`, `lineMemo?`).
- `POST {referenceNumber}/reversal`: reverse. Requires `REVERSE_MANUAL_JOURNAL`. Body: `reason`.
- `GET`: list, newest first, with lines, capped at the 500 most recent journals. Requires `VIEW_MANUAL_JOURNAL`. Each line carries `accountName` and `accountType`. Each journal carries
  `reversalOfReferenceNumber` and `reversedByReferenceNumber`.

Validation failures return HTTP 400 through the global handler. `message` joins the messages, and `body` is the array. The messages are:

- "Summary is required." / "Reason is required."
- "At least two lines are required."
- "Account {accountCode} can't be used in a manual journal."
- "Account {accountCode} is not postable."
- "Debits must equal credits."
- "No open fiscal period for {postingDate}."

Reversal also reports "A reversal cannot be reversed." and "This manual journal has already been reversed."

## Permission

`RtsPermission.CREATE_MANUAL_JOURNAL` (`CMJ`) gates create, and `RtsPermission.REVERSE_MANUAL_JOURNAL` (`RMJ`) gates reverse. Both are organization tier. `RtsRole.MANAGE_MANUAL_JOURNALS` (`MMJR`) bundles both. The GET requires `RtsPermission.VIEW_MANUAL_JOURNAL` (`VMJ`).

## Known gaps

- Each create posts a new journal. Two submissions of the same entry are two valid journals; a duplicate is corrected by reversing it.
- Not yet covered by tests beyond `ArchitectureTest`, and not yet run against a database. All manual journal changesets are
  version 2 (`labels: 2.0.0`).
- Not a delivery fix. A bad delivery needs a document-level reversal, because purchase counters, payment status, and
  stock layers all derive from the delivery lines.
