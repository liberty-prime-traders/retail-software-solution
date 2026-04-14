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
import java.time.temporal.ChronoUnit

@Service
@TransactionalOnOrganizationSchema
class FiscalPeriodGenerationService(
    private val configService: OrgAccountingConfigService,
    private val repository: FiscalPeriodRepository,
    private val strategyRegistry: PeriodCycleStrategyRegistry
) {

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
                val adjustmentEnd = cleanStart.minusDays(1)
                val clampedEnd = saveAdjustmentPeriod(nextStart, adjustmentEnd, config)
                if (clampedEnd.isBefore(adjustmentEnd)) {
                    saveAdjustmentPeriod(clampedEnd.plusDays(1), adjustmentEnd, config)
                }
                val adjustedContext = buildContext(config, cleanStart)
                savePeriod(cleanStart, strategy.nextPeriod(cleanStart.minusDays(1), adjustedContext), config, adjustedContext)
            } else {
                savePeriod(nextStart, strategy.nextPeriod(lastPeriodEnd, context), config, context)
            }
        }
    }

    private fun savePeriod(
        naturalStart: LocalDate,
        range: PeriodRange,
        config: OrgAccountingConfigDto,
        context: PeriodGenerationContext
    ) {
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(naturalStart, config.fiscalYearEndMonth, config.fiscalYearEndDay)
        val yearEnd = !range.end.isBefore(fiscalYearEnd)
        val clampedEnd = if (yearEnd) fiscalYearEnd else range.end

        val periodNumber = context.existingPeriodsInCurrentYear + 1
        val name = FiscalPeriodNameGenerator.generate(naturalStart, config.fiscalPeriodCycle, periodNumber)

        repository.save(FiscalPeriodEntity(
            name = name,
            startDate = naturalStart,
            endDate = clampedEnd,
            yearEnd = yearEnd
        ))
    }

    private fun saveAdjustmentPeriod(start: LocalDate, end: LocalDate, config: OrgAccountingConfigDto): LocalDate {
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth, config.fiscalYearEndDay)
        val clampedEnd = if (end.isAfter(fiscalYearEnd)) fiscalYearEnd else end
        repository.save(FiscalPeriodEntity(
            name = "Adj ${start.monthValue}/${start.year}",
            startDate = start,
            endDate = clampedEnd,
            adjustmentPeriod = true,
            yearEnd = clampedEnd == fiscalYearEnd
        ))
        return clampedEnd
    }

    private fun buildContext(config: OrgAccountingConfigDto, forDate: LocalDate): PeriodGenerationContext {
        val yearEnd = FiscalPeriodUtils.yearEnd(forDate, config.fiscalYearEndMonth, config.fiscalYearEndDay)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        val count = repository.countByStartDateGreaterThanEqualAndStartDateLessThanEqual(yearStart, yearEnd)
        return PeriodGenerationContext(config, count)
    }
}
