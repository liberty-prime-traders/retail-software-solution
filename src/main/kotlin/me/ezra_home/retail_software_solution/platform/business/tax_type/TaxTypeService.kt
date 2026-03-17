package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.dto.TaxTypeUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class TaxTypeService(
    private val taxTypeMapper: TaxTypeMapper,
    private val taxTypeCache: TaxTypeCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<TaxTypeResponseDto> =
        taxTypeCache.getAll().map { taxTypeMapper.toResponseDto(it) }

    fun create(dto: TaxTypeInsertDto): TaxTypeResponseDto {
        StringUtils.getValueOrException(dto.name, "Name must not be blank")
        if (taxTypeCache.getAll().any { StringUtils.isEquivalent(it.name, dto.name) })
            throw RtsGenericException("A tax type named '${dto.name}' already exists")
        val entity = taxTypeMapper.toEntity(dto)
        taxTypeCache.upsert(entity)
        return taxTypeMapper.toResponseDto(entity)
    }

    fun update(dto: TaxTypeUpdateDto): TaxTypeResponseDto {
        val entity = taxTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        taxTypeMapper.partialUpdate(dto, entity)
        StringUtils.getValueOrException(entity.name, "Name must not be blank")
        if (taxTypeCache.getAll().any { StringUtils.isEquivalent(it.name, entity.name) && it.id != entity.id })
            throw RtsGenericException("A tax type named '${entity.name}' already exists")
        taxTypeCache.upsert(entity)
        return taxTypeMapper.toResponseDto(entity)
    }

    fun delete(id: UUID) = taxTypeCache.delete(id)
}
