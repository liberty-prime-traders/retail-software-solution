package me.ezra_home.retail_software_solution.organizations.business.accounting_config.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.OrgAccountingConfigCache
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.OrgAccountingConfigMapper
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.DayOfWeek

@Service
@TransactionalOnOrganizationSchema
class OrgAccountingConfigService(
    private val configCache: OrgAccountingConfigCache,
    private val mapper: OrgAccountingConfigMapper
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun get(): OrgAccountingConfigResponseDto {
        val config = configCache.get() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        return mapper.toResponseDto(config)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getConfig(): OrgAccountingConfigDto? = configCache.get()

    fun initialize() {
        if (configCache.get() != null) return
        val today = DateTimes.Local.Now.organization()
        configCache.create(OrgAccountingConfigInsertDto(
            fiscalYearEndMonth = today.monthValue,
            fiscalYearEndDay = today.dayOfMonth,
            fiscalPeriodCycle = FiscalPeriodCycle.MONTHLY,
            periodWeekStartDay = DayOfWeek.MONDAY,
            periodPrepDays = 2
        ))
    }

    fun update(dto: OrgAccountingConfigUpdateDto): OrgAccountingConfigResponseDto {
        val existing = configCache.get() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        val updated = configCache.save(dto.applyTo(existing))
        return mapper.toResponseDto(updated)
    }
}
