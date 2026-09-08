package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.BulkUnitImportValidator
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.UnitValueDependencyGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionService
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.SystemUnitValue
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueBulkSaveEntry
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class BulkUnitImportService(
    private val bulkUnitImportValidator: BulkUnitImportValidator,
    private val unitGroupService: UnitGroupService,
    private val unitValueService: UnitValueService,
    private val unitValueFetcher: UnitValueFetcher,
    private val unitConversionService: UnitConversionService
) {

    fun bulkImport(bulkUnitImportRequestDto: BulkUnitImportRequestDto): Collection<UnitGroupResponseDto> {
        val errors = bulkUnitImportValidator.validate(bulkUnitImportRequestDto)
        if (errors.isNotEmpty()) {
            throw RtsGenericException("Bulk unit import validation failed", errors)
        }

        val savedGroups = unitGroupService.bulkCreateValidatedList(
            bulkUnitImportRequestDto.unitGroups.map { UnitGroupInsertDto(name = it.name!!, description = it.description) }
        )
        val groupIdByName = savedGroups.associate { (StringUtils.getValueOrNull(it.name) ?: "") to it.id }

        saveValidatedUnitValues(bulkUnitImportRequestDto, groupIdByName)
        saveConversions(bulkUnitImportRequestDto)

        return savedGroups
    }

    /** One save-order node: a unit value pending creation for a specific group. Codes are unique
     * org-wide (including auto-provisioned Piece codes, which fold the group name in - see
     * SystemUnitValue.pieceCodeForGroup), so the code alone identifies a node for ordering. */
    private data class PendingUnitValue(
        val name: String,
        val code: String,
        val description: String?,
        val unitGroupId: UUID,
        val baseUnitCode: String?,
        val conversionFactor: BigDecimal?
    )

    private fun saveValidatedUnitValues(
        bulkUnitImportRequestDto: BulkUnitImportRequestDto,
        groupIdByName: Map<String, UUID>
    ): List<UnitValueResponseDto> {
        val pending : List<PendingUnitValue> = bulkUnitImportRequestDto.unitGroups.flatMap { group ->
            val groupId = groupIdByName.getValue(group.name!!)
            generatePendingUnitValuesForGroup(groupId, group.name, group.unitValues)
        }

        val resolvedIds = mutableMapOf<String, UUID>()
        unitValueFetcher.getAllUnitValues().forEach { resolvedIds[it.code] = it.id }

        val orderedPending = topologicallyOrder(pending)
        val topologicallyOrderedEntries = orderedPending.map { pendingValue ->
            val id = UUID.randomUUID()
            resolvedIds[pendingValue.code] = id
            UnitValueBulkSaveEntry(
                id = id,
                name = pendingValue.name,
                code = pendingValue.code,
                description = pendingValue.description,
                unitGroupId = pendingValue.unitGroupId,
                baseUnit = pendingValue.baseUnitCode?.let { resolvedIds[it] },
                conversionFactor = pendingValue.conversionFactor
            )
        }
        return unitValueService.bulkCreateValidatedList(topologicallyOrderedEntries)
    }

    private fun generatePendingUnitValuesForGroup(groupId: UUID, groupName: String?, unitValues: List<UnitValueBulkInsertDto>): List<PendingUnitValue> {
        val excludedFromPiece = listOf(SystemUnitGroup.MISC, SystemUnitGroup.WEIGHT, SystemUnitGroup.VOLUME)
            .any { StringUtils.isEquivalent(it.groupName, groupName) }
        val referencesPiece = unitValues.any { it.baseUnitCode == BulkUnitImportValidator.PIECE_BASE_UNIT_CODE }
        val pieceCode = SystemUnitValue.pieceCodeForGroup(groupName)
        val pieceAlreadyExists = unitValueFetcher.getUnitValuesForUnitGroup(groupId).any { it.code == pieceCode }

        return buildList {
            if (referencesPiece && !excludedFromPiece && !pieceAlreadyExists) {
                add(createDefaultPiece(groupId, pieceCode))
            }
            unitValues.forEach {
                add(createPendingUnitValue(groupId, pieceCode, it))
            }
        }
    }

    private fun createDefaultPiece(groupId: UUID, pieceCode: String) = PendingUnitValue(
        name = SystemUnitValue.PIECE.unitName,
        code = pieceCode,
        description = "Base unit representing a single item",
        unitGroupId = groupId,
        baseUnitCode = null,
        conversionFactor = null
    )

    private fun createPendingUnitValue(groupId: UUID, pieceCode: String, unitValueDto: UnitValueBulkInsertDto): PendingUnitValue {
        val baseUnitCode = when (unitValueDto.baseUnitCode) {
            BulkUnitImportValidator.PIECE_BASE_UNIT_CODE -> pieceCode
            else -> unitValueDto.baseUnitCode
        }
        return PendingUnitValue(
            name = unitValueDto.name!!,
            code = unitValueDto.code!!,
            description = unitValueDto.description,
            unitGroupId = groupId,
            baseUnitCode = baseUnitCode,
            conversionFactor = unitValueDto.unitsOfBasePerUnit?.toBigDecimal()
        )
    }

    /** Base-unit chains are self-referential FKs, so a single saveAll must insert parents before
     * children - this reorders the pending batch accordingly (already proven acyclic in Phase 1). */
    private fun topologicallyOrder(pending: List<PendingUnitValue>): List<PendingUnitValue> {
        val byCode = pending.associateBy { it.code }
        val graph = UnitValueDependencyGraph()
        byCode.keys.forEach { graph.addNode(it) }
        pending.forEach { value ->
            if (value.baseUnitCode != null && byCode.containsKey(value.baseUnitCode)) {
                graph.addEdge(value.baseUnitCode, value.code)
            }
        }
        return graph.topologicalOrder().map { byCode.getValue(it) }
    }

    private fun saveConversions(bulkUnitImportRequestDto: BulkUnitImportRequestDto): List<UnitConversionDto> {
        if (bulkUnitImportRequestDto.unitConversions.isEmpty()) return emptyList()
        val idByCode = unitValueFetcher.getAllUnitValues().associate { it.code to it.id }

        val insertDtos = bulkUnitImportRequestDto.unitConversions.map {
            UnitConversionInsertDto(
                fromUnitId = idByCode.getValue(it.fromUnitCode!!),
                toUnitId = idByCode.getValue(it.toUnitCode!!),
                factor = it.unitsOfBasePerUnit!!
            )
        }
        return unitConversionService.bulkInsertValidatedList(insertDtos)
    }
}
