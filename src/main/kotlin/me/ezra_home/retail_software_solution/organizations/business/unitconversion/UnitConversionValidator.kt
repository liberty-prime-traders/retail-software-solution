package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class UnitConversionValidator(
    private val unitValueFetcher: UnitValueFetcher,
    private val unitConversionRepository: UnitConversionRepository
) {

    fun validateInsert(dto: UnitConversionInsertDto) {
        if (dto.numerator <= 0 || dto.denominator <= 0) {
            throw RtsGenericException("numerator and denominator must both be greater than zero")
        }
        if (dto.fromUnitId == dto.toUnitId) throw RtsGenericException("fromUnitId and toUnitId must be different")

        val allUnits = unitValueFetcher.getAllUnitValues().associateBy { it.id }
        val fromUnit = allUnits[dto.fromUnitId] ?: throw RtsGenericException("fromUnitId does not exist")
        val toUnit = allUnits[dto.toUnitId] ?: throw RtsGenericException("toUnitId does not exist")

        if (fromUnit.unitGroupId == toUnit.unitGroupId) {
            throw RtsGenericException("Units in the same group are already connected via the base unit chain")
        }

        if (unitConversionRepository.existsByFromUnitIdAndToUnitId(dto.fromUnitId, dto.toUnitId) ||
            unitConversionRepository.existsByFromUnitIdAndToUnitId(dto.toUnitId, dto.fromUnitId)) {
            throw RtsGenericException("A conversion between these two units already exists")
        }
    }

    fun validateUpdate(dto: UnitConversionUpdateDto) {
        if (dto.numerator <= 0 || dto.denominator <= 0) {
            throw RtsGenericException("numerator and denominator must both be greater than zero")
        }
    }
}
