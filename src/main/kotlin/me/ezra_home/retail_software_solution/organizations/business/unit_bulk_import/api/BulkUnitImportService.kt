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

        // StringUtils.isEquivalent's normalization is a pure function of the name, so a name -> group
        // map keyed by that same normalization gives every lookup below O(1) instead of an O(n) scan.
        val existingSystemDefinedGroupsByNormalizedName = unitGroupService.getAllUnitGroups()
            .filter { it.systemDefined }
            .associateBy { StringUtils.normalizeForComparison(it.name) }
        fun findExistingSystemDefinedGroup(name: String?) =
            existingSystemDefinedGroupsByNormalizedName[StringUtils.normalizeForComparison(name ?: "")]

        val groupsToCreate = bulkUnitImportRequestDto.unitGroups.filter { findExistingSystemDefinedGroup(it.name) == null }
        val createdGroups = unitGroupService.bulkCreateValidatedList(
            groupsToCreate.map { UnitGroupInsertDto(name = it.name!!, description = it.description) }
        )
        val createdGroupIdByName = createdGroups.associate { (StringUtils.getValueOrNull(it.name) ?: "") to it.id }

        val groupIdByName = bulkUnitImportRequestDto.unitGroups.associate { group ->
            val name = group.name!!
            name to (findExistingSystemDefinedGroup(name)?.id ?: createdGroupIdByName.getValue(name))
        }

        saveValidatedUnitValues(bulkUnitImportRequestDto, groupIdByName)
        saveConversions(bulkUnitImportRequestDto)

        val reusedGroups = bulkUnitImportRequestDto.unitGroups.mapNotNull { findExistingSystemDefinedGroup(it.name) }
        return createdGroups + reusedGroups
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
        val unitsOfBasePerUnit: Long?
    )

    private fun saveValidatedUnitValues(
        bulkUnitImportRequestDto: BulkUnitImportRequestDto,
        groupIdByName: Map<String, UUID>
    ): List<UnitValueResponseDto> {
        val existingUnitValues = unitValueFetcher.getAllUnitValues()
        val existingSystemDefinedCodes = existingUnitValues.filter { it.systemDefined }.map { it.code }.toSet()

        val pending : List<PendingUnitValue> = bulkUnitImportRequestDto.unitGroups.flatMap { group ->
            val groupId = groupIdByName.getValue(group.name!!)
            generatePendingUnitValuesForGroup(groupId, group.name, group.unitValues, existingSystemDefinedCodes)
        }

        val resolvedIds = mutableMapOf<String, UUID>()
        existingUnitValues.forEach { resolvedIds[it.code] = it.id }

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
                unitsOfBasePerUnit = pendingValue.unitsOfBasePerUnit
            )
        }
        return unitValueService.bulkCreateValidatedList(topologicallyOrderedEntries)
    }

    private fun generatePendingUnitValuesForGroup(
        groupId: UUID,
        groupName: String?,
        unitValues: List<UnitValueBulkInsertDto>,
        existingSystemDefinedCodes: Set<String>
    ): List<PendingUnitValue> {
        val excludedFromPiece = SystemUnitGroup.isExcludedFromPieceAutoInsert(groupName)
        val referencesPiece = unitValues.any { it.baseUnitCode == BulkUnitImportValidator.PIECE_BASE_UNIT_CODE }
        val pieceCode = SystemUnitValue.pieceCodeForGroup(groupName)
        val pieceAlreadyExists = unitValueFetcher.getUnitValuesForUnitGroup(groupId).any { it.code == pieceCode }

        return buildList {
            if (referencesPiece && !excludedFromPiece && !pieceAlreadyExists) {
                add(createDefaultPiece(groupId, pieceCode))
            }
            // A unit value whose code already matches a system-defined row (see SystemUnitValue /
            // UnitValueSeeder) was accepted by the validator as a reuse, not a duplicate - its id is
            // already resolvable via the existing-unit-value lookup, so it must not be inserted again.
            unitValues.filter { it.code !in existingSystemDefinedCodes }.forEach {
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
        unitsOfBasePerUnit = null
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
            unitsOfBasePerUnit = unitValueDto.unitsOfBasePerUnit
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
                numerator = it.numerator!!,
                denominator = it.denominator!!
            )
        }
        return unitConversionService.bulkInsertValidatedList(insertDtos)
    }
}
