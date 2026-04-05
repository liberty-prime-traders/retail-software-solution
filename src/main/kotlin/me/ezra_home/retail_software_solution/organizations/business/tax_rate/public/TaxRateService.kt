package me.ezra_home.retail_software_solution.organizations.business.tax_rate.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.public.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateCache
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateMapper
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateValidator
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto.TaxRateDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeResolver
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class TaxRateService(
    private val taxRateMapper: TaxRateMapper,
    private val taxRateCache: TaxRateCache,
    private val taxRateValidator: TaxRateValidator,
    private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache,
    private val jurisdictionTaxTypeResolver: JurisdictionTaxTypeResolver
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): Collection<TaxRateResponseDto> {
        val platformIndex = jurisdictionTaxTypeResolver.buildIndex()
        val parentMap = orgJurisdictionTaxTypeCache.getAll().associateBy { it.id }
        return taxRateCache.getAll().map { taxRateDto ->
            val parent = parentMap[taxRateDto.orgJurisdictionTaxTypeId]
            val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
            val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
            taxRateMapper.toResponseDto(taxRateDto, taxLabel, parentIsActive)
        }
    }

    fun create(dto: TaxRateInsertDto): TaxRateResponseDto {
        taxRateValidator.validateForCreate(dto)
        val taxRateDto = taxRateMapper.toDomainDto(dto)
        val savedDto = taxRateCache.save(taxRateDto)
        return toResponseDto(savedDto)
    }

    fun update(dto: TaxRateUpdateDto): TaxRateResponseDto {
        if (dto.name == null && dto.endDate == null) throw RtsGenericException("Nothing to update")
        val taxRateDto = taxRateCache.getAll().firstOrNull { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        taxRateValidator.validateForUpdate(taxRateDto, dto)
        if (dto.name != null) taxRateDto.name = dto.name.orElse(null)!!
        if (dto.endDate != null) taxRateDto.endDate = dto.endDate.orElse(null)
        val savedDto = taxRateCache.save(taxRateDto)
        return toResponseDto(savedDto)
    }

    private fun toResponseDto(taxRateDto: TaxRateDto): TaxRateResponseDto {
        val platformIndex = jurisdictionTaxTypeResolver.buildIndex()
        val parent = orgJurisdictionTaxTypeCache.getAll().firstOrNull { it.id == taxRateDto.orgJurisdictionTaxTypeId }
        val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
        val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
        return taxRateMapper.toResponseDto(taxRateDto, taxLabel, parentIsActive)
    }
}
