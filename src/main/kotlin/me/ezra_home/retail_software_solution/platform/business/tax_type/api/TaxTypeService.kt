package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class TaxTypeService(
    private val taxTypeMapper: TaxTypeMapper,
    private val taxTypeCache: TaxTypeCache,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<TaxTypeResponseDto> =
        taxTypeCache.getAll().map { taxTypeMapper.toResponseDto(it) }

    fun create(dto: TaxTypeInsertDto): TaxTypeResponseDto {
        StringUtils.requireHasValue(dto.name, "Name must not be blank")
        if (dto.taxTriggers.isEmpty()) throw RtsGenericException("At least one tax trigger is required")
        if (taxTypeCache.getAll().any { StringUtils.isEquivalent(it.name, dto.name) })
            throw RtsGenericException("A tax type named '${dto.name}' already exists")
        val saved = taxTypeCache.create(dto)
        return taxTypeMapper.toResponseDto(saved)
    }

    fun update(dto: TaxTypeUpdateDto): TaxTypeResponseDto {
        val existing = taxTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        val effectiveName = dto.name?.orElse(existing.name) ?: existing.name
        StringUtils.requireHasValue(effectiveName, "Name must not be blank")
        if (dto.taxTriggers != null && dto.taxTriggers.orElse(emptySet())?.isEmpty() == true)
            throw RtsGenericException("At least one tax trigger is required")
        if (taxTypeCache.getAll().any { StringUtils.isEquivalent(it.name, effectiveName) && it.id != existing.id })
            throw RtsGenericException("A tax type named '$effectiveName' already exists")
        val updated = dto.applyTo(existing)
        val saved = taxTypeCache.save(updated)
        return taxTypeMapper.toResponseDto(saved)
    }

    fun delete(id: UUID) {
        if (jurisdictionTaxTypeFetcher.isInUse(id))
            throw RtsGenericException("Tax type is in use and cannot be deleted")
        taxTypeCache.delete(id)
    }
}
