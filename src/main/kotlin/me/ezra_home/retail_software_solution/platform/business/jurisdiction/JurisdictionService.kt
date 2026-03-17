package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionResponseDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto.JurisdictionUpdateDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class JurisdictionService(
    private val jurisdictionMapper: JurisdictionMapper,
    private val jurisdictionCache: JurisdictionCache,
    private val jurisdictionValidator: JurisdictionValidator,
    private val jurisdictionTaxTypeService: JurisdictionTaxTypeService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<JurisdictionResponseDto> =
        jurisdictionCache.getAll().map { jurisdictionMapper.toResponseDto(it) }

    fun create(dto: JurisdictionInsertDto): JurisdictionResponseDto {
        jurisdictionValidator.validateName(dto.name)
        jurisdictionValidator.validateTypeExists(dto.jurisdictionTypeId)
        jurisdictionValidator.validateParent(dto.parentJurisdictionId)
        val entity = jurisdictionMapper.toEntity(dto)
        jurisdictionCache.upsert(entity)
        dto.taxTypesToAddOrReactivate
            ?.map { JurisdictionTaxTypeInsertDto(it, entity.id!!) }
            ?.let { jurisdictionTaxTypeService.createAll(it) }
        return jurisdictionMapper.toResponseDto(entity)
    }

    fun update(dto: JurisdictionUpdateDto): JurisdictionResponseDto {
        val entity = jurisdictionCache.getAll().find { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        dto.jurisdictionTypeId?.let { jurisdictionTypeId ->
            jurisdictionTypeId.ifPresent { jurisdictionValidator.validateTypeExists(it) }
        }
        jurisdictionValidator.validateParent(dto.parentJurisdictionId?.orElse(null), dto.id)
        jurisdictionMapper.partialUpdate(dto, entity)
        jurisdictionValidator.validateName(entity.name)
        jurisdictionCache.upsert(entity)
        dto.taxTypesToAddOrReactivate?.let {
            jurisdictionTaxTypeService.addOrReactivate(dto.id, it)
        }
        dto.taxTypesToDiscontinue?.let {
            jurisdictionTaxTypeService.stopByTaxTypeIds(dto.id, it)
        }
        return jurisdictionMapper.toResponseDto(entity)
    }

    fun delete(id: UUID) = jurisdictionCache.delete(id)
}
