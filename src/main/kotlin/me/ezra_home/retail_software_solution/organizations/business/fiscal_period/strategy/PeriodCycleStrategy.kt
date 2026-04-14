package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import java.time.LocalDate

interface PeriodCycleStrategy {

    fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange

    /**
     * Returns the first date on or after [from] that is a clean cycle boundary (e.g., first of month for MONTHLY).
     * If [from] is already a clean start, returns [from] itself.
     * Used to detect when an adjustment period is needed after a cycle change.
     */
    fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate
}
