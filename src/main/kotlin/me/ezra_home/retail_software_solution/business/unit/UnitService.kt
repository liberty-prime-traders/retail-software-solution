package me.ezra_home.retail_software_solution.business.unit

import java.util.Objects
import java.util.UUID
import me.ezra_home.retail_software_solution.business.unit.dto.UnitInsertDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitResponseDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UnitService(
    private val unitMapper: UnitMapper,
    private val unitCache: UnitCache
) {

    @Transactional
    fun getAllUnits(): Collection<UnitResponseDto> {
        return unitCache.getAllUnits().map { unitMapper.toDto(it) }
    }

    @Transactional
    fun createUnit(unitInsertDto: UnitInsertDto): UnitResponseDto {
        val existingUnits = unitCache.getAllUnits()

        UnitValidator.validateInsert(unitInsertDto, existingUnits)

        val newUnitEntity = unitMapper.toEntity(unitInsertDto)
        val savedUnitEntity = unitCache.upsertUnit(newUnitEntity)
        return unitMapper.toDto(savedUnitEntity)
    }

    @Transactional
    fun updateUnit(unitDto: UnitUpdateDto): UnitResponseDto {
        val existingUnits = unitCache.getAllUnits()

        val existingUnit = existingUnits.find { it.id == unitDto.id }
            ?: throw UpdatingNonExistingRecordException()

        UnitValidator.validateUpdate(unitDto, existingUnit, existingUnits)

        unitMapper.partialUpdate(unitDto, existingUnit)
        val updatedUnit = unitCache.upsertUnit(existingUnit)
        return unitMapper.toDto(updatedUnit)
    }

    @Transactional
    fun deleteUnit(id: UUID?) {
        id?.let {
            val entity = unitCache.getAllUnits().find { it.id == id }
                ?: throw RtsGenericException("Unit with ID $id not found.")

            if (entity.usageCount > 0L) {
                throw RtsGenericException("Unit '${entity.name}' has ${entity.usageCount} usage(s) and cannot be deleted.")
            }
            unitCache.deleteUnit(id)
        }
    }
}

