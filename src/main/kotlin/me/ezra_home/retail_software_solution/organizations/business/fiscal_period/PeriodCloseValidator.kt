package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class PeriodCloseValidator(
    private val repository: FiscalPeriodRepository,
    private val configService: OrgAccountingConfigService
) {

    fun assertCanClose(period: FiscalPeriodEntity) {
        if (period.isClosed) throw RtsGenericException("Period '${period.name}' is already closed")
        if (!isClosable(period.endDate)) throw RtsGenericException("Period '${period.name}' has not ended yet and cannot be closed")
        if (period.yearEnd && hasOpenSiblings(period.id!!, period.endDate)) {
            throw RtsGenericException("All periods in the fiscal year must be closed before closing '${period.name}'")
        }
    }

    fun isClosable(dto: FiscalPeriodDto): Boolean {
        if (dto.isClosed || !isClosable(dto.endDate)) return false
        if (!dto.yearEnd) return true
        return !hasOpenSiblings(dto.id, dto.endDate)
    }

    fun isClosable(dto: FiscalPeriodDto, allPeriodsInYear: Collection<FiscalPeriodDto>): Boolean {
        if (dto.isClosed || !isClosable(dto.endDate)) return false
        if (!dto.yearEnd) return true
        return allPeriodsInYear.none { it.id != dto.id && !it.isClosed }
    }

    private fun hasOpenSiblings(id: UUID, endDate: LocalDate): Boolean {
        val config = configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        val yearEnd = FiscalPeriodUtils.yearEnd(endDate, config.fiscalYearEndMonth, config.fiscalYearEndDay)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        return repository.countOpenInYear(yearStart, yearEnd, id) > 0
    }

    private fun isClosable(endDate: LocalDate): Boolean {
        val today = DateTimes.Local.Now.organization()
        return today.isAfter(endDate)
    }
}
