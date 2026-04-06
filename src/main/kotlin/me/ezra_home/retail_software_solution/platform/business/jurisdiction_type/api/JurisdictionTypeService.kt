package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.JurisdictionTypeCache
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.JurisdictionTypeMapper
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
        val saved = jurisdictionTypeCache.create(dto)
        return jurisdictionTypeMapper.toResponseDto(saved)
    }

    fun update(dto: JurisdictionTypeUpdateDto): JurisdictionTypeResponseDto {
        val existing = jurisdictionTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        val updated = dto.applyTo(existing)
        StringUtils.getValueOrException(updated.name, "Name must not be blank")
        if (jurisdictionTypeCache.getAll().any { StringUtils.isEquivalent(it.name, updated.name) && it.id != updated.id })
            throw RtsGenericException("A jurisdiction type named '${updated.name}' already exists")
        val saved = jurisdictionTypeCache.save(updated)
        return jurisdictionTypeMapper.toResponseDto(saved)
    }

    fun delete(id: UUID) = jurisdictionTypeCache.delete(id)
}
