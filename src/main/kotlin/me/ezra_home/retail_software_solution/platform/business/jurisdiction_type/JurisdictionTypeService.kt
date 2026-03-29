package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeResponseDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto.JurisdictionTypeUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionTypeService(
    private val jurisdictionTypeMapper: JurisdictionTypeMapper,
    private val jurisdictionTypeCache: JurisdictionTypeCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<JurisdictionTypeResponseDto> =
        jurisdictionTypeCache.getAll().map { jurisdictionTypeMapper.toResponseDto(it) }

    fun create(dto: JurisdictionTypeInsertDto): JurisdictionTypeResponseDto {
        StringUtils.getValueOrException(dto.name, "Name must not be blank")
        if (jurisdictionTypeCache.getAll().any { StringUtils.isEquivalent(it.name, dto.name) })
            throw RtsGenericException("A jurisdiction type named '${dto.name}' already exists")
        val entity = jurisdictionTypeMapper.toEntity(dto)
        jurisdictionTypeCache.upsert(entity)
        return jurisdictionTypeMapper.toResponseDto(entity)
    }

    fun update(dto: JurisdictionTypeUpdateDto): JurisdictionTypeResponseDto {
        val entity = jurisdictionTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        jurisdictionTypeMapper.partialUpdate(dto, entity)
        StringUtils.getValueOrException(entity.name, "Name must not be blank")
        if (jurisdictionTypeCache.getAll().any { StringUtils.isEquivalent(it.name, entity.name) && it.id != entity.id })
            throw RtsGenericException("A jurisdiction type named '${entity.name}' already exists")
        jurisdictionTypeCache.upsert(entity)
        return jurisdictionTypeMapper.toResponseDto(entity)
    }

    fun delete(id: UUID) = jurisdictionTypeCache.delete(id)
}
