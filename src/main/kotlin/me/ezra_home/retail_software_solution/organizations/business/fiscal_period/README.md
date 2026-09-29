# fiscal_period

Owns the calendar of fiscal periods an organization posts accounting activity into.
Periods are generated forward from the last existing period, named per cycle, and
closed individually (or as a year-end roll-up).

## Domain

- `FiscalPeriodEntity` — `name`, `startDate`, `endDate` (LocalDate, **inclusive**),
  `yearEnd`, `stub`, `closedAt`, `closedBy`. Audited; date columns are `@NotAudited`
  and not updatable.
- `FiscalPeriodDto` — internal domain projection produced by `FiscalPeriodMapper`.
- `api/FiscalPeriodResponseDto` — outbound projection enriched with `fiscalYear`,
  `closable`, `current`, and resolved `closedBy` full name.

## Generation

`FiscalPeriodGenerationService.generatePeriods()` walks forward from
`max(endDate)` (or `today - 1` on a fresh org) until the latest end is at least
`config.periodPrepDays` ahead of today. Each step asks the cycle's
`PeriodCycleStrategy` for the next clean boundary:

- if `nextCleanStart` is past `nextStart`, the gap is filled with a **stub**
  period named `Stub <MMM yyyy>`.
- otherwise, `nextPeriod(lastEnd, config)` returns a `PeriodRange` for the
  natural cycle window.
- the resulting end is clamped to `FiscalPeriodUtils.yearEnd(naturalStart, ...)`
  when it would otherwise cross the fiscal year-end; that period is flagged
  `yearEnd = true`.

`FiscalPeriodGenerationJob` runs `generatePeriods` nightly per organization
(`cron = "0 0 1 * * *"`), and `FiscalPeriodService.nudgePeriodGeneration()`
exposes the same call for the "Nudge" button.

### Cycle strategies (`strategy/`)

| Cycle          | Strategy                      |
|----------------|-------------------------------|
| WEEKLY         | `WeeklyPeriodStrategy`        |
| MONTHLY        | `MonthlyPeriodStrategy`       |
| QUARTERLY      | `QuarterlyPeriodStrategy`     |
| SEMI_ANNUAL    | `SemiAnnualPeriodStrategy`    |
| ANNUAL         | `AnnualPeriodStrategy`        |
| FOUR_FOUR_FIVE | `FourFourFivePeriodStrategy`  |

`PeriodCycleStrategyRegistry.get(cycle)` resolves the strategy.
`PeriodRange` is a typealias for `DateTimes.DateRange`.

## Closing

- `FiscalPeriodService.close(ids)` — closes one or more **non-year-end** periods.
  Year-end periods route through `YearEndCloseService.close(id)`, which also
  triggers `DenormalizedYearEndBalanceTransfer.applyYearEndBalanceTransfer()`
  before stamping `closedAt`/`closedBy`.
- `PeriodCloseValidator.assertCanClose` requires:
  - not already closed,
  - `today.isAfter(endDate)` (period has fully elapsed),
  - if `yearEnd`, every other period inside the same fiscal year is closed.

## Lookups

- `findOpenForDate(date)` / `requireOpenForDate(date)` — used by sale/purchase
  posting to refuse activity outside an open period. Uses `startDate <= date <= endDate`
  (inclusive both ends) and ignores closed periods.
- `getAll()` returns response DTOs with `current` computed via
  `DateTimes.todayIsInRange(start, end)`.
- `rename(...)` blocks duplicate names within the same fiscal year (case-insensitive
  via `StringUtils.isEquivalent`).

## Naming

`FiscalPeriodNameGenerator`:

- `MONTHLY` → `MMM yyyy` (e.g. `May 2026`)
- `QUARTERLY` → `Q<n> <fyStartYear>`
- `SEMI_ANNUAL` → `H<n> <fyStartYear>`
- `ANNUAL` → `FY <fyStartYear>`
- `WEEKLY` → `W<nn> <fyStartYear>`
- `FOUR_FOUR_FIVE` → `P<nn> FY<fyStartYear>`
- stub → `Stub <MMM yyyy>` (overrides cycle name)

## Fiscal year helpers (`FiscalPeriodUtils`)

- `yearEnd(date, endMonth)` — last day of the fiscal year that contains `date`.
- `yearStart(yearEnd)` — `yearEnd.minusYears(1).plusDays(1)`.
- `fiscalYearLabel` — `FY<year>` when start/end share a calendar year,
  `FY<startYear>/<endYearLast2>` otherwise.
- `positionIn445` — 0/1/2 slot inside a 4-4-5 13-week group.

## Known bug — Apr 30 8 AM → May 1 8 AM appears uncovered

System-generated periods on UTC+8 org:

- `Stub Apr 2026`: start `Apr 26`, end `Apr 30`
- `May 2026`:      start `May 1`,  end `May 31`

Generation is correct — `Apr 30` belongs to the Stub period (inclusive), and
the next period starts the very next day. `FiscalPeriodRepository.findOpenContaining`
also uses `startDate <= :date AND endDate >= :date`, so backend lookups for any
calendar day in that range resolve to a period.

The "gap" is a **serialization** problem. `FiscalPeriodResponseDto.endDate`
is a `LocalDate`, serialized as `"2026-04-30"`. The browser parses that as
`2026-04-30T00:00:00Z` (midnight UTC) and renders in the org's UTC+8 zone as
`Apr 30 8:00 AM`. An **inclusive end-of-day** thereby displays as the **start
of that day**, leaving the wall-clock window `Apr 30 8:00 AM → May 1 8:00 AM`
apparently uncovered.

**Source:** UI-side rendering of `FiscalPeriodResponseDto.endDate` (`LocalDate`,
inclusive). The backend contract is **inclusive whole calendar day** — the cycle
strategies, `findOpenContaining`, and `PeriodCloseValidator` all rely on that
semantic and are correct.

**Fix (UI):** render `startDate` / `endDate` as the date string the API
returns (no `new Date(...)`, no time component, no timezone conversion). The
value is a calendar day, not a moment in time. Do not change the response shape.
