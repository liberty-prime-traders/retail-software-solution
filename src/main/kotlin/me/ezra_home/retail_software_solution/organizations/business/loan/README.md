# loan

Money the organization borrows from a contact (an owner, a bank, a relative), recorded as a liability, and repayments
made against a specific loan. Organization schema; every endpoint requires the `CHART_OF_ACCOUNTS` feature
(`secured/loans`). The shape mirrors an expense and its payments: the loan is the document, repayments are payments
against it, and every void is its own append-only row.

## Model

- `loan` (`LOAN`), `loan_repayment` (`LNRP`), `loan_void` (`LNVD`) and `loan_repayment_void` (`LRPV`) are immutable and
  append-only, so none is audited. A void is unique per loan or repayment and carries `void_date` (always today in the
  organization zone, never user-supplied) and an optional reason. Corrections are void and re-enter; there is no update.
- `lender_contact_id`, `loan_id` and `loan_repayment_id` are real foreign keys, as are both `asset_account_code`
  columns. `idx_loan_created_on_id` backs the keyset search; `idx_loan_lender_contact_id` backs the lender FK; `idx_loan_repayment_loan_id` backs the repayment lookups.
- Lenders are contacts holding `ContactType.LENDER`. `LoanLenderValidator` resolves the lender through the cached by-reference contact map (`ContactService.findContactByReferenceNumber`) and requires it to exist, be `ACTIVE` and hold
  `LENDER`; it runs at loan creation only. Repayments and voids use the loan's lender and never re-check its type or status.
- The liability side is fixed: `SystemAccount.LOANS_PAYABLE`, never in a request. The user picks the asset account that
  received the money (loan) or paid it back (repayment); each goes through `LoanAccountValidator`, which calls
  `AccountSelectionService.requireSelectable(AccountSelectionRule.LOAN, code)`.
- A repayment is always against one loan (`loanId`) and never splits across loans.

## Outstanding balance and status

Derived by the `loan_status` view (`views/loan_status.yml`, named by `ViewNames.LOAN_STATUS`), exposed as the read-only `LoanStatusEntity` through
`LoanStatusRepository`; there is no state row. `outstanding` = loan amount − active repayments, except a voided loan, which
reports `0` (its void posts the whole amount back out of `LOANS_PAYABLE`). `LOANS_PAYABLE` therefore equals the sum of
`outstanding` over all loans. Status is stored as a code (`LoanStatus`): `VOI` when a void row exists, else `STL` when
`outstanding` is zero, else `OPN`. A repayment is active when it has no `loan_repayment_void` row; that rule lives only in the
`active_loan_repayment` view (`ActiveLoanRepaymentEntity`), which `loan_status` sums from.

The repayment outstanding check (under the loan lock), `LoanResponseBuilder.buildResponses` (search and `getLoan`) and
`LoanSearchQueryBuilder`, which joins `loan_status` for the `statuses` filter, read the view. A write never re-reads it: the
view does not see the transaction's pending writes. `LoanFinder.findLoanStatusFigures` reads the status once under the loan
lock, before the write, and the service moves it with `LoanStatusResolver` (`forNewLoan`, `afterRepayment`,
`afterRepaymentVoid`, `afterLoanVoid`) and passes the resulting `LoanStatusFigures` to `LoanResponseBuilder.buildResponse`.
`LoanStatusResolver` mirrors the view's rules, so a change to `loan_status.yml` changes it too. The void-loan guard lists active repayments through
`ActiveLoanRepaymentRepository`. The response builder still loads every repayment and its void row because it renders each
repayment's own `void`.

## Rules

- Validation is in the services and their validators, not on the DTOs. `LoanService` handles loans and loan voids;
  `LoanRepaymentService` handles repayments and repayment voids. Amounts are `PositiveMoney` (zero, a negative, more decimals than the session money scale, or a magnitude above
  `Decimals.MAXIMUM_TRANSACTION_AMOUNT` are rejected when the request binds, never rounded), and dates through `DateTimes.Local.requireNotInFuture`; repayment rules in `LoanRepaymentValidator`,
  loan-void rules in `LoanVoidValidator`, and the lock, lookup and lender reference in `LoanFinder`. Notes and void
  reasons are blank-to-null and unbounded (`text`).
- `loanDate` and `repaymentDate` cannot be after today in the organization zone, and a repayment cannot be dated before
  its loan's `loanDate`, so `LOANS_PAYABLE` is never debited ahead of the credit it repays.
- Recording a repayment, voiding a loan and voiding a repayment all take `OrgEntityAdvisoryLock` on the loan id
  (`LockNamespaces.LOAN`) before reading anything the decision depends on, and before `LedgerPostingService.post` takes its
  account and contact locks. A repayment above the outstanding amount names the amount requested and the outstanding amount.
- A loan cannot be voided while it has a non-voided repayment; the error lists their references. A repayment can be voided
  at any time: a voided loan has only voided repayments, so the repayment's own void check already covers it.
- The open fiscal period and account postability are enforced by `LedgerPostingService.post`, not repeated here.
- `LoanAccountUsageProvider` reports an asset account used by a loan or a repayment (newest ten references of each) to
  `AccountUsagesFinder`, so such an account cannot gain children or be deactivated; a void against a non-leaf account
  would be rejected because accounts with children never post directly.
- `LOANS_PAYABLE` takes no manual opening balance (`opening_balance/README.md`).

## Ledger posting is synchronous

Unlike the event-driven posting of the other domains, the service saves the row and then calls
`LedgerPostingService.post` in the same organization-schema transaction. A posting failure rolls the row back and reaches
the caller. There are no Kafka events, processors or reissue handlers. `LoanPostingRequests` builds all four requests; the
subledger entry is for the loan's lender. A void reads the original row's asset account and amount, never the current
account configuration. Postings and subledger columns are listed in `ledger/README.md`.

## Responses and search

- Responses are ready to render: lender reference and display name, account `code` and `label`, `amountRepaid`,
  `outstanding`, `status`, the recorded-by name, `void` (null, or `voidDate`, `reason`, `voidedBy`) and every repayment
  with its own `void`. `LoanResponse` and `LoanRepaymentResponse` carry `id` because the void routes take it.
  `LoanResponseBuilder` loads repayments, voids and labels once per request over the whole page, and takes the lenders as a
  contact map from the caller (`ContactService.getContactsById` for a page, the one known lender otherwise).
- Routes: `POST` (loan), `POST repayments`, `POST void`, `POST repayments/void`, `POST search`, `GET {loanReference}`.
  A repayment or void returns the loan.
- `POST search` follows `cross_tier/expense/README.md`: keyset on `(created_on, id)` descending, one extra row for
  `hasMore`, the query returns only `(id, created_on)` and the page is shaped by the response builder. `createdFrom` /
  `createdBefore` are required, the upper bound is exclusive and the span is at most one year.
- Filters: `lenderContactReferences`, `statuses` (all or none means no filter), `loanDateFrom` / `loanDateBefore` and
  `references` (blanks dropped). The open loans of a lender are `lenderContactReferences = [lender]` with
  `statuses = [OPEN]` over a bounded range. There is no summary search.
