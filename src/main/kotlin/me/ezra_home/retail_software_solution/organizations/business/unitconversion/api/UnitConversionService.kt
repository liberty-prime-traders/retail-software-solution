package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionEntity
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionRepository
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionValidator
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitConversionService(
    private val unitConversionRepository: UnitConversionRepository,
    private val unitConversionValidator: UnitConversionValidator,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<UnitConversionDto> =
        unitConversionRepository.findAll().map { it.toDto() }

    fun insert(dto: UnitConversionInsertDto): UnitConversionDto {
        unitConversionValidator.validateInsert(dto)
        val entity = UnitConversionEntity(
            fromUnitId = dto.fromUnitId,
            toUnitId = dto.toUnitId,
            factor = dto.factor
        )
        val saved = unitConversionRepository.saveAndFlush(entity)
        invalidateGraph()
        return saved.toDto()
    }

    fun update(dto: UnitConversionUpdateDto): UnitConversionDto {
        unitConversionValidator.validateUpdate(dto)
        val entity = unitConversionRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
        entity.factor = dto.factor
        val saved = unitConversionRepository.save(entity)
        invalidateGraph()
        return saved.toDto()
    }

    fun delete(id: UUID) {
        unitConversionRepository.deleteById(id)
        invalidateGraph()
    }

    fun invalidateGraph() {
        unitConversionGraphFacade.invalidate()
    }

    private fun UnitConversionEntity.toDto() = UnitConversionDto(
        id = id!!,
        fromUnitId = fromUnitId,
        toUnitId = toUnitId,
        factor = factor
    )
}
