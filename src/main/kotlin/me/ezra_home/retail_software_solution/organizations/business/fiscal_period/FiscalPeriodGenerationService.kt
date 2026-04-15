package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy.PeriodCycleStrategyRegistry
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy.PeriodGenerationContext
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy.PeriodRange
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
@TransactionalOnOrganizationSchema
class FiscalPeriodGenerationService(
    private val configService: OrgAccountingConfigService,
    private val repository: FiscalPeriodRepository,
    private val strategyRegistry: PeriodCycleStrategyRegistry
) {

    private val stubMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    fun generatePeriods() {
        val config = configService.getConfig() ?: return
        val today = DateTimes.Local.Now.organization()

        while (true) {
            val lastPeriodEnd = repository.findTopByOrderByEndDateDesc()?.endDate ?: today.minusDays(1)
            if (ChronoUnit.DAYS.between(today, lastPeriodEnd) > config.periodPrepDays) break

            val nextStart = lastPeriodEnd.plusDays(1)
            val strategy = strategyRegistry.get(config.fiscalPeriodCycle)
            val context = buildContext(config, nextStart)

            val cleanStart = strategy.nextCleanStart(nextStart, context)
            if (cleanStart.isAfter(nextStart)) {
                savePeriod(nextStart, PeriodRange(nextStart, cleanStart.minusDays(1)), config, context, stub = true)
            } else {
                savePeriod(nextStart, strategy.nextPeriod(lastPeriodEnd, context), config, context, stub = false)
            }
        }
    }

    private fun buildContext(config: OrgAccountingConfigDto, forDate: LocalDate): PeriodGenerationContext {
        val yearEnd = FiscalPeriodUtils.yearEnd(forDate, config.fiscalYearEndMonth)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        val count = repository.countFullPeriodsForDates(yearStart, yearEnd)
        return PeriodGenerationContext(config, count)
    }

    private fun savePeriod(
        naturalStart: LocalDate,
        range: PeriodRange,
        config: OrgAccountingConfigDto,
        context: PeriodGenerationContext,
        stub: Boolean
    ) {
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(naturalStart, config.fiscalYearEndMonth)
        val isYearEnd = !range.end.isBefore(fiscalYearEnd)
        val clampedEnd = if (isYearEnd) fiscalYearEnd else range.end
        val name = if (stub) "Stub ${naturalStart.format(stubMonthFormatter)}"
                   else getPeriodName(naturalStart, context)
        repository.save(FiscalPeriodEntity(
            name = name,
            startDate = naturalStart,
            endDate = clampedEnd,
            yearEnd = isYearEnd,
            stub = stub
        ))
    }

    private fun getPeriodName(naturalStart: LocalDate, context: PeriodGenerationContext): String {
        val periodNumber = context.existingPeriodsInCurrentYear + 1
        return FiscalPeriodNameGenerator.generate(naturalStart, context.config.fiscalPeriodCycle, periodNumber, context.config.fiscalYearEndMonth)
    }
}
