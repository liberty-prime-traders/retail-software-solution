package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodGenerationService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodMapper
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodRepository
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.PeriodCloseValidator
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class FiscalPeriodService(
    private val repository: FiscalPeriodRepository,
    private val mapper: FiscalPeriodMapper,
    private val configService: OrgAccountingConfigService,
    private val fiscalPeriodGenerationService: FiscalPeriodGenerationService,
    private val validator: PeriodCloseValidator,
    private val userFullNameQualifier: UserQualifier
) {


    fun nudgePeriodGeneration() {
        fiscalPeriodGenerationService.generatePeriods()
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<FiscalPeriodResponseDto> {
        val config = configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        val dtos = repository.findAll().map { mapper.toDomainDto(it) }
        val periodsByYearEnd = dtos.groupBy { FiscalPeriodUtils.yearEnd(it.endDate, config.fiscalYearEndMonth, config.fiscalYearEndDay) }
        return dtos.map { dto ->
            val yearPeers = periodsByYearEnd[FiscalPeriodUtils.yearEnd(dto.endDate, config.fiscalYearEndMonth, config.fiscalYearEndDay)].orEmpty()
            toResponseDto(dto, config, validator.isClosable(dto, yearPeers))
        }
    }

    fun close(ids: Set<UUID>): List<FiscalPeriodResponseDto> {
        val now = Instant.now()
        val periods = repository.findByIdIn(ids)
        periods.forEach { period ->
            if (period.yearEnd) {
                throw RtsGenericException("Period '${period.name}' is a year-end period — use the year-end close endpoint")
            }
            validator.assertCanClose(period)
            period.closedAt = now
            period.closedBy = SessionContextProvider.getUserId()
        }
        val config = configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        return repository.saveAll(periods).map {
            dto -> toResponseDto(mapper.toDomainDto(dto), config, false)
        }
    }

    fun rename(fiscalPeriodRenameDto: FiscalPeriodRenameDto): FiscalPeriodResponseDto {
        val id = fiscalPeriodRenameDto.id
        val name = fiscalPeriodRenameDto.name
        val period = repository.findById(id).orElseThrow { RtsGenericException("Fiscal period not found: $id") }
        val config = configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        if (StringUtils.isEquivalent(period.name, name)) {
            val dto = mapper.toDomainDto(period)
            return toResponseDto(dto, config, validator.isClosable(dto))
        }
        val yearEnd = FiscalPeriodUtils.yearEnd(period.startDate, config.fiscalYearEndMonth, config.fiscalYearEndDay)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        repository.findPeriodsInGivenYear(yearStart, yearEnd)
            .filter { StringUtils.isEquivalent(it.name, name) }
            .let {
                if (it.isNotEmpty()) {
                    throw RtsGenericException("A period named '$name' already exists in this fiscal year")
                }
            }
        period.name = name
        val savedDto = mapper.toDomainDto(repository.save(period))
        return toResponseDto(savedDto, config, validator.isClosable(savedDto))
    }

    private fun toResponseDto(
        dto: FiscalPeriodDto,
        config: OrgAccountingConfigDto,
        closable: Boolean
    ): FiscalPeriodResponseDto {
        return FiscalPeriodResponseDto(
            id = dto.id,
            name = dto.name,
            fiscalYear = FiscalPeriodUtils.fiscalYearLabel(dto.endDate, config.fiscalYearEndMonth, config.fiscalYearEndDay),
            startDate = dto.startDate,
            endDate = dto.endDate,
            yearEnd = dto.yearEnd,
            adjustmentPeriod = dto.adjustmentPeriod,
            closedAt = dto.closedAt,
            closedBy = userFullNameQualifier.getUserFullName(dto.closedBy),
            closable = closable
        )
    }
}
