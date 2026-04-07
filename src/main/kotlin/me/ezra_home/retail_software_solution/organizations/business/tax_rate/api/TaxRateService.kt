package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateCache
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateMapper
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateValidator
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class TaxRateService(
    private val taxRateMapper: TaxRateMapper,
    private val taxRateCache: TaxRateCache,
    private val taxRateValidator: TaxRateValidator,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeService: JurisdictionTaxTypeService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): Collection<TaxRateResponseDto> {
        val platformIndex = jurisdictionTaxTypeService.buildIndex()
        val parentMap = orgJurisdictionTaxTypeFetcher.getAllDtos().associateBy { it.id }
        return taxRateCache.getAll().map { taxRateDto ->
            val parent = parentMap[taxRateDto.orgJurisdictionTaxTypeId]
            val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
            val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
            taxRateMapper.toResponseDto(taxRateDto, taxLabel, parentIsActive)
        }
    }

    fun create(dto: TaxRateInsertDto): TaxRateResponseDto {
        taxRateValidator.validateForCreate(dto)
        val savedDto = taxRateCache.create(dto)
        return toResponseDto(savedDto)
    }

    fun update(dto: TaxRateUpdateDto): TaxRateResponseDto {
        if (dto.name == null && dto.endDate == null) throw RtsGenericException("Nothing to update")
        val existing = taxRateCache.getAll().firstOrNull { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        taxRateValidator.validateForUpdate(existing, dto)
        val updated = dto.applyTo(existing)
        val savedDto = taxRateCache.save(updated)
        return toResponseDto(savedDto)
    }

    private fun toResponseDto(taxRateDto: TaxRateDto): TaxRateResponseDto {
        val platformIndex = jurisdictionTaxTypeService.buildIndex()
        val parent = orgJurisdictionTaxTypeFetcher.getAllDtos().firstOrNull { it.id == taxRateDto.orgJurisdictionTaxTypeId }
        val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
        val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
        return taxRateMapper.toResponseDto(taxRateDto, taxLabel, parentIsActive)
    }
}
