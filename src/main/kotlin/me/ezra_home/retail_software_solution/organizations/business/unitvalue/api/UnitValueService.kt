package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueCache
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueEntity
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

    fun bulkCreateValidatedList(topologicallyOrderedEntries: List<UnitValueBulkSaveEntry>): List<UnitValueResponseDto> {
        if (topologicallyOrderedEntries.isEmpty()) return emptyList()
        val entities = topologicallyOrderedEntries.map { entry ->
            UnitValueEntity(
                name = entry.name,
                description = entry.description,
                code = entry.code,
                unitGroupId = entry.unitGroupId,
                baseUnit = entry.baseUnit,
                unitsOfBasePerUnit = entry.unitsOfBasePerUnit,
                systemDefined = false
            ).also { it.id = entry.id }
        }
        val saved = unitValueCache.saveAll(entities)
        unitConversionGraphFacade.invalidate()
        val unitNamesById = unitValueCache.getUnitNamesById()
        return saved.map {
            val dto = unitValueMapper.toDomainDto(it)
            unitValueMapper.toResponseDto(dto, unitNamesById[it.baseUnit])
        }
    }

    /** Provisions the group's default Piece base unit unless the group is Miscellaneous, Weight,
     * or Volume (those have their own base units) or a Piece unit already exists in the group. */
    fun ensurePieceUnitExists(unitGroupId: UUID, unitGroupName: String?): UnitValueResponseDto? {
        val excludedFromPiece = SystemUnitGroup.isExcludedFromPieceAutoInsert(unitGroupName)
        if (excludedFromPiece) return null

        val pieceCode = SystemUnitValue.pieceCodeForGroup(unitGroupName)
        if (unitValueCache.getByUnitGroupId(unitGroupId).any { it.code == pieceCode }) return null

        val saved = unitValueCache.save(
            UnitValueEntity(
                name = SystemUnitValue.PIECE.unitName,
                code = pieceCode,
                unitGroupId = unitGroupId,
                systemDefined = false
            )
        )
        unitConversionGraphFacade.invalidate()
        return unitValueMapper.toResponseDto(saved, null)
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
