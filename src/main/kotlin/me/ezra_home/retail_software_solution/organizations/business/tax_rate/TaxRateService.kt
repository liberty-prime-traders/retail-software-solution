package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto.TaxRateInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto.TaxRateResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.dto.TaxRateUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.TaxRateEntity
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
        return taxRateCache.getAll().map { entity ->
            val parent = parentMap[entity.orgJurisdictionTaxTypeId]
            val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
            val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
            taxRateMapper.toResponseDto(entity, taxLabel, parentIsActive)
        }
    }

    fun create(dto: TaxRateInsertDto): TaxRateResponseDto {
        taxRateValidator.validateForCreate(dto)
        val entity = taxRateMapper.toEntity(dto)
        taxRateCache.save(entity)
        return toResponseDto(entity)
    }

    fun update(dto: TaxRateUpdateDto): TaxRateResponseDto {
        if (dto.name == null && dto.endDate == null) throw RtsGenericException("Nothing to update")
        val entity = taxRateCache.getAll().firstOrNull { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        taxRateValidator.validateForUpdate(entity, dto)
        if (dto.name != null) entity.name = dto.name.orElse(null)
        if (dto.endDate != null) entity.endDate = dto.endDate.orElse(null)
        taxRateCache.save(entity)
        return toResponseDto(entity)
    }

    private fun toResponseDto(entity: TaxRateEntity): TaxRateResponseDto {
        val platformIndex = jurisdictionTaxTypeResolver.buildIndex()
        val parent = orgJurisdictionTaxTypeCache.getAll().firstOrNull { it.id == entity.orgJurisdictionTaxTypeId }
        val taxLabel = parent?.let { platformIndex[it.jurisdictionTaxTypeId]?.label }
        val parentIsActive = parent?.status == OrgJurisdictionTaxTypeStatus.ACTIVE
        return taxRateMapper.toResponseDto(entity, taxLabel, parentIsActive)
    }
}
