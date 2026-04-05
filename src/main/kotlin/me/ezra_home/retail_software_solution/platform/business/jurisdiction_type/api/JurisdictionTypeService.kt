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
        val jurisdictionTypeDto = jurisdictionTypeMapper.toDomainDto(dto)
        jurisdictionTypeCache.upsert(jurisdictionTypeDto)
        return jurisdictionTypeMapper.toResponseDto(jurisdictionTypeDto)
    }

    fun update(dto: JurisdictionTypeUpdateDto): JurisdictionTypeResponseDto {
        val jurisdictionTypeDto = jurisdictionTypeCache.getAll().find { it.id == dto.id }
            ?: throw UpdatingNonExistingRecordException()
        jurisdictionTypeMapper.partialUpdate(dto, jurisdictionTypeDto)
        StringUtils.getValueOrException(jurisdictionTypeDto.name, "Name must not be blank")
        if (jurisdictionTypeCache.getAll().any { StringUtils.isEquivalent(it.name, jurisdictionTypeDto.name) && it.id != jurisdictionTypeDto.id })
            throw RtsGenericException("A jurisdiction type named '${jurisdictionTypeDto.name}' already exists")
        jurisdictionTypeCache.upsert(jurisdictionTypeDto)
        return jurisdictionTypeMapper.toResponseDto(jurisdictionTypeDto)
    }

    fun delete(id: UUID) = jurisdictionTypeCache.delete(id)
}
