package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy.PeriodCycleStrategyRegistry
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
        var lastPeriodEnd = repository.findTopByOrderByEndDateDesc()?.endDate ?: today.minusDays(1)

        while (true) {
            if (ChronoUnit.DAYS.between(today, lastPeriodEnd) > config.periodPrepDays) break
            val nextStart = lastPeriodEnd.plusDays(1)
            val strategy = strategyRegistry.get(config.fiscalPeriodCycle)
            val cleanStart = strategy.nextCleanStart(nextStart, config)
            lastPeriodEnd = if (cleanStart.isAfter(nextStart)) {
                savePeriod(nextStart, PeriodRange(nextStart, cleanStart.minusDays(1)), config, stub = true)
            } else {
                savePeriod(nextStart, strategy.nextPeriod(lastPeriodEnd, config), config, stub = false)
            }
        }
    }

    private fun savePeriod(
        naturalStart: LocalDate,
        range: PeriodRange,
        config: OrgAccountingConfigDto,
        stub: Boolean
    ): LocalDate {
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(naturalStart, config.fiscalYearEndMonth)
        val isYearEnd = !range.end.isBefore(fiscalYearEnd)
        val clampedEnd = if (isYearEnd) fiscalYearEnd else range.end
        val name = if (stub) FiscalPeriodNameGenerator.stubName(naturalStart)
                   else FiscalPeriodNameGenerator.generate(naturalStart, config)
        repository.save(FiscalPeriodEntity(
            name = name,
            startDate = naturalStart,
            endDate = clampedEnd,
            yearEnd = isYearEnd,
            stub = stub
        ))
        return clampedEnd
    }
}
