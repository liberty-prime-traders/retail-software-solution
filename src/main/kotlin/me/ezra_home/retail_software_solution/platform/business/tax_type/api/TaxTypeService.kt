package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
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
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<TaxTypeResponseDto> =
        taxTypeCache.getAll().map { taxTypeMapper.toResponseDto(it) }

    fun create(dto: TaxTypeInsertDto): TaxTypeResponseDto {
        StringUtils.getValueOrException(dto.name, "Name must not be blank")
        if (dto.taxTriggers.isEmpty()) throw RtsGenericException("At least one tax trigger is required")
        if (taxTypeCache.getAll().any { StringUtils.isEquivalent(it.name, dto.name) })
            throw RtsGenericException("A tax type named '${dto.name}' already exists")
        val taxTypeDto = taxTypeMapper.toDomainDto(dto)
        taxTypeCache.upsert(taxTypeDto)
        return taxTypeMapper.toResponseDto(taxTypeDto)
    }

    fun update(dto: TaxTypeUpdateDto): TaxTypeResponseDto {
        val taxTypeDto = taxTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        val effectiveName = if (dto.name != null) dto.name.orElse(null) else taxTypeDto.name
        StringUtils.getValueOrException(effectiveName, "Name must not be blank")
        if (dto.taxTriggers != null && dto.taxTriggers.orElse(emptyList()).isEmpty())
            throw RtsGenericException("At least one tax trigger is required")
        if (taxTypeCache.getAll().any { StringUtils.isEquivalent(it.name, effectiveName) && it.id != taxTypeDto.id })
            throw RtsGenericException("A tax type named '$effectiveName' already exists")
        taxTypeMapper.partialUpdate(dto, taxTypeDto)
        taxTypeCache.upsert(taxTypeDto)
        return taxTypeMapper.toResponseDto(taxTypeDto)
    }

    fun delete(id: UUID) {
        if (jurisdictionTaxTypeCache.getAll().any { it.taxTypeId == id })
            throw RtsGenericException("Tax type is in use and cannot be deleted")
        taxTypeCache.delete(id)
    }
}
