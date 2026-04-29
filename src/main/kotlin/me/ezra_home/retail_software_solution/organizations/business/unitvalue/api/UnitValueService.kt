package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueCache
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueMapper
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitValueService(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper,
    private val unitValueValidator: UnitValueValidator,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueInsert(unitValueInsertDto)
        val dto = unitValueCache.create(unitValueInsertDto)
        unitConversionGraphFacade.invalidate()
        return unitValueMapper.toResponseDto(dto, unitValueCache.getUnitNamesById()[dto.baseUnit])
    }

    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueUpdate(unitValueUpdateDto)
        val existing = unitValueCache.getAllUnitValues().find { Objects.equals(it.id, unitValueUpdateDto.id) }
            ?: throw UpdatingNonExistingRecordException()
        if (existing.systemDefined) throw RtsGenericException("System-defined unit values cannot be modified")
        val updated = unitValueUpdateDto.applyTo(existing)
        val saved = unitValueCache.save(updated)
        unitConversionGraphFacade.invalidate()
        return unitValueMapper.toResponseDto(saved, unitValueCache.getUnitNamesById()[saved.baseUnit])
    }

    fun deleteUnitValue(id: UUID?) {
        unitValueCache.getAllUnitValues()
            .find { it.id == id }
            ?.apply {
                if (systemDefined) throw RtsGenericException("System-defined unit values cannot be deleted")
                unitValueCache.deleteUnitValue(id)
                unitConversionGraphFacade.invalidate()
            }
    }
}
