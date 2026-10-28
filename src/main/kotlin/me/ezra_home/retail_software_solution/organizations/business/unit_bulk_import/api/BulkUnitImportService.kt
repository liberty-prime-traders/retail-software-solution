package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.BulkUnitImportValidator
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.UnitValueDependencyGraph
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.RootUnitDefinition
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
    private val unitValueFetcher: UnitValueFetcher
) {

    fun bulkImport(bulkUnitImportRequestDto: BulkUnitImportRequestDto): Collection<UnitGroupResponseDto> {
        val errors = bulkUnitImportValidator.validate(bulkUnitImportRequestDto)
        if (errors.isNotEmpty()) {
            throw RtsGenericException("Bulk unit import validation failed", errors)
        }

        val existingSystemDefinedGroupsByNormalizedName = unitGroupService.getAllUnitGroups()
            .filter { it.systemDefined }
            .associateBy { StringUtils.normalizeForComparison(it.name) }
        fun findExistingSystemDefinedGroup(name: String?) =
            existingSystemDefinedGroupsByNormalizedName[StringUtils.normalizeForComparison(name ?: "")]

        val groupsToCreate = bulkUnitImportRequestDto.unitGroups.filter { findExistingSystemDefinedGroup(it.name) == null }
        val createdGroups = unitGroupService.bulkCreateValidatedList(
            groupsToCreate.map { UnitGroupInsertDto(name = it.name!!, description = it.description) }
        )
        val createdGroupIdByNameKey = createdGroups.associate { StringUtils.alphanumericKey(it.name) to it.id }

        val groupIdByName = bulkUnitImportRequestDto.unitGroups.associate { group ->
            val name = group.name!!
            name to (findExistingSystemDefinedGroup(name)?.id ?: createdGroupIdByNameKey.getValue(StringUtils.alphanumericKey(name)))
        }

        saveValidatedUnitValues(bulkUnitImportRequestDto, groupIdByName)

        val reusedGroups = bulkUnitImportRequestDto.unitGroups.mapNotNull { findExistingSystemDefinedGroup(it.name) }
        return createdGroups + reusedGroups
    }

    /** One save-order node: a unit value pending creation for a specific group. Codes are unique
     * org-wide (including root unit codes, see RootUnitDefinition.effectiveBaseUnitCode), so the
     * code alone identifies a node for ordering. */
    private data class PendingUnitValue(
        val name: String,
        val code: String,
        val description: String?,
        val unitGroupId: UUID,
        val baseUnitCode: String?,
        val unitsOfBasePerUnit: Long?,
        val systemDefined: Boolean = false
    )

    private fun saveValidatedUnitValues(
        bulkUnitImportRequestDto: BulkUnitImportRequestDto,
        groupIdByName: Map<String, UUID>
    ): List<UnitValueResponseDto> {
        val existingUnitValues = unitValueFetcher.getAllUnitValues()
        val existingSystemDefinedCodes = existingUnitValues.filter { it.systemDefined }.map { it.code }.toSet()

        val pending : List<PendingUnitValue> = bulkUnitImportRequestDto.unitGroups.flatMap { group ->
            val groupId = groupIdByName.getValue(group.name!!)
            generatePendingUnitValuesForGroup(groupId, group, existingSystemDefinedCodes)
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
                unitsOfBasePerUnit = pendingValue.unitsOfBasePerUnit,
                systemDefined = pendingValue.systemDefined
            )
        }
        return unitValueService.bulkCreateValidatedList(topologicallyOrderedEntries)
    }

    private fun generatePendingUnitValuesForGroup(
        groupId: UUID,
        unitGroupBulkInsertDto: UnitGroupBulkInsertDto,
        existingSystemDefinedCodes: Set<String>
    ): List<PendingUnitValue> {
        val excludedFromBaseUnit = SystemUnitGroup.isExcludedFromRootAutoInsert(unitGroupBulkInsertDto.name)
        val rootUnitDefinition = RootUnitDefinition.forGroup(
            unitGroupId = groupId,
            unitGroupName = unitGroupBulkInsertDto.name,
            baseUnitName = unitGroupBulkInsertDto.baseUnitName,
            baseUnitCode = unitGroupBulkInsertDto.baseUnitCode
        )

        return buildList {
            if (!excludedFromBaseUnit) {
                add(createPendingRootUnit(rootUnitDefinition))
            }
            // A unit value whose code already matches a system-defined row (see SystemUnitValue /
            // UnitValueSeeder) was accepted by the validator as a reuse, not a duplicate - its id is
            // already resolvable via the existing-unit-value lookup, so it must not be inserted again.
            unitGroupBulkInsertDto.unitValues.filter { it.code !in existingSystemDefinedCodes }.forEach {
                add(createPendingUnitValue(groupId, rootUnitDefinition.code, it))
            }
        }
    }

    private fun createPendingRootUnit(rootUnitDefinition: RootUnitDefinition) = PendingUnitValue(
        name = rootUnitDefinition.name,
        code = rootUnitDefinition.code,
        description = rootUnitDefinition.description,
        unitGroupId = rootUnitDefinition.unitGroupId,
        baseUnitCode = null,
        unitsOfBasePerUnit = null,
        systemDefined = rootUnitDefinition.systemDefined
    )

    private fun createPendingUnitValue(groupId: UUID, rootUnitCode: String, unitValueDto: UnitValueBulkInsertDto): PendingUnitValue {
        val baseUnitCode = when (unitValueDto.baseUnitCode) {
            RootUnitDefinition.PIECE_BASE_UNIT_CODE -> rootUnitCode
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
}
