package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.DenormalizedYearEndBalanceTransfer
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodMapper
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodRepository
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.PeriodCloseValidator
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class YearEndCloseService(
    private val repository: FiscalPeriodRepository,
    private val mapper: FiscalPeriodMapper,
    private val configService: OrgAccountingConfigService,
    private val denormalizedYearEndBalanceTransfer: DenormalizedYearEndBalanceTransfer,
    private val validator: PeriodCloseValidator,
    private val userFullNameQualifier: UserQualifier
) {

    fun close(periodId: UUID): FiscalPeriodResponseDto {
        val config = configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        val period = repository.findById(periodId).orElseThrow {
            RtsGenericException("Fiscal period not found: $periodId")
        }

        if (!period.yearEnd) throw RtsGenericException("Period '${period.name}' is not a year-end period")
        validator.assertCanClose(period)

        denormalizedYearEndBalanceTransfer.applyYearEndBalanceTransfer()

        period.closedAt = Instant.now()
        period.closedBy = SessionContextProvider.getUserId()

        val dto = mapper.toDomainDto(repository.save(period))
        return FiscalPeriodResponseDto(
            id = dto.id,
            name = dto.name,
            fiscalYear = FiscalPeriodUtils.fiscalYearLabel(dto.endDate, config.fiscalYearEndMonth),
            startDate = dto.startDate,
            endDate = dto.endDate,
            yearEnd = dto.yearEnd,
            stub = dto.stub,
            closedAt = dto.closedAt,
            closedBy = userFullNameQualifier.getUserFullName(dto.closedBy),
            closable = false,
            current = false
        )
    }
}
