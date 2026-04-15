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
        val config = requireConfig()
        val dtos = repository.findAll().map { mapper.toDomainDto(it) }
        val periodsByYearEnd = dtos.groupBy { FiscalPeriodUtils.yearEnd(it.endDate, config.fiscalYearEndMonth) }
        return dtos.map { dto ->
            val yearEnd = FiscalPeriodUtils.yearEnd(dto.endDate, config.fiscalYearEndMonth)
            val yearPeers = periodsByYearEnd[yearEnd].orEmpty()
            toResponseDto(dto, config, validator.isClosable(dto, yearPeers))
        }
    }

    fun close(ids: Set<UUID>): List<FiscalPeriodResponseDto> {
        val now = Instant.now()
        val periods = repository.findByIdIn(ids)
        val missingIds = ids - periods.mapNotNull { it.id }.toSet()
        if (missingIds.isNotEmpty()) throw RtsGenericException("Fiscal periods not found: $missingIds")
        periods.forEach { period ->
            if (period.yearEnd) {
                throw RtsGenericException("Period '${period.name}' is a year-end period — use the year-end close endpoint")
            }
            validator.assertCanClose(period)
            period.closedAt = now
            period.closedBy = SessionContextProvider.getUserId()
        }
        val config = requireConfig()
        return repository.saveAll(periods).map {
            dto -> toResponseDto(mapper.toDomainDto(dto), config, false)
        }
    }

    fun rename(fiscalPeriodRenameDto: FiscalPeriodRenameDto): FiscalPeriodResponseDto {
        val id = fiscalPeriodRenameDto.id
        val name = fiscalPeriodRenameDto.name
        val period = repository.findById(id).orElseThrow { RtsGenericException("Fiscal period not found: $id") }
        val config = requireConfig()
        if (StringUtils.isEquivalent(period.name, name)) {
            val dto = mapper.toDomainDto(period)
            return toResponseDto(dto, config, validator.isClosable(dto))
        }
        val yearEnd = FiscalPeriodUtils.yearEnd(period.startDate, config.fiscalYearEndMonth)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        val nameConflict = repository.findPeriodsInGivenYear(yearStart, yearEnd)
            .any { StringUtils.isEquivalent(it.name, name) }
        if (nameConflict) throw RtsGenericException("A period named '$name' already exists in this fiscal year")
        period.name = name
        val savedDto = mapper.toDomainDto(repository.save(period))
        return toResponseDto(savedDto, config, validator.isClosable(savedDto))
    }

    private fun requireConfig(): OrgAccountingConfigDto =
        configService.getConfig() ?: throw RtsGenericException("Accounting configuration has not been initialized")

    private fun toResponseDto(
        dto: FiscalPeriodDto,
        config: OrgAccountingConfigDto,
        closable: Boolean
    ): FiscalPeriodResponseDto {
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
            closable = closable
        )
    }
}
