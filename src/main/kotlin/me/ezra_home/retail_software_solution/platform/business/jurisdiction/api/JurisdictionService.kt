package me.ezra_home.retail_software_solution.platform.business.jurisdiction.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionCache
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionMapper
import me.ezra_home.retail_software_solution.platform.business.jurisdiction.JurisdictionValidator
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeService
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
        val jurisdictionDto = jurisdictionMapper.toDomainDto(dto)
        jurisdictionCache.upsert(jurisdictionDto)
        dto.taxTypesToAddOrReactivate
            ?.map { JurisdictionTaxTypeInsertDto(it, jurisdictionDto.getNullSafeId()) }
            ?.let { jurisdictionTaxTypeService.createAll(it) }
        return jurisdictionMapper.toResponseDto(jurisdictionDto)
    }

    fun update(dto: JurisdictionUpdateDto): JurisdictionResponseDto {
        val jurisdictionDto = jurisdictionCache.getAll().find { it.id == dto.id } ?: throw UpdatingNonExistingRecordException()
        dto.jurisdictionTypeId?.let { jurisdictionTypeId ->
            jurisdictionTypeId.ifPresent { jurisdictionValidator.validateTypeExists(it) }
        }
        jurisdictionValidator.validateParent(dto.parentJurisdictionId?.orElse(null), dto.id)
        jurisdictionMapper.partialUpdate(dto, jurisdictionDto)
        jurisdictionValidator.validateName(jurisdictionDto.name)
        jurisdictionCache.upsert(jurisdictionDto)
        dto.taxTypesToAddOrReactivate?.let {
            jurisdictionTaxTypeService.addOrReactivate(dto.id, it)
        }
        dto.taxTypesToDiscontinue?.let {
            jurisdictionTaxTypeService.stopByTaxTypeIds(dto.id, it)
        }
        return jurisdictionMapper.toResponseDto(jurisdictionDto)
    }

    fun delete(id: UUID) = jurisdictionCache.delete(id)
}
