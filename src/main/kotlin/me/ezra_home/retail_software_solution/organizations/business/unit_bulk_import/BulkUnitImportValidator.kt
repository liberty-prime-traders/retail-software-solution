package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.BulkUnitImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitValueBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.SystemUnitValue
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitChainNode
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitChainResolver
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BulkUnitImportValidator(
    private val unitGroupService: UnitGroupService,
    private val unitValueFetcher: UnitValueFetcher
) {

    fun validate(bulkUnitImportRequestDto: BulkUnitImportRequestDto): List<String> {
        val errors = mutableListOf<String>()
        val existingGroups = unitGroupService.getAllUnitGroupDtos()
        val existingUnitValues = unitValueFetcher.getAllUnitValues()
        val payloadGroups = bulkUnitImportRequestDto.unitGroups

        validateGroupNames(payloadGroups, existingGroups, errors)
        validateGeneratedPieceCodes(payloadGroups, existingUnitValues, existingGroups, errors)

        val groupNameByCode = buildGroupNameByCode(payloadGroups, existingUnitValues, existingGroups)
        validateUnitValues(payloadGroups, existingUnitValues, existingGroups, groupNameByCode, errors)
        validateNoCycles(payloadGroups, existingUnitValues, groupNameByCode, errors)
        if (errors.isEmpty()) {
            validateChainSizes(payloadGroups, existingUnitValues, errors)
        }

        return errors
    }

    private fun validateGroupNames(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingGroups: Collection<UnitGroupDto>,
        errors: MutableList<String>
    ) {
        payloadGroups.forEachIndexed { index, group ->
            if (!StringUtils.hasValue(group.name)) {
                errors.add("Group at position ${index + 1}: name is required")
                return@forEachIndexed
            }
            val name = group.name!!
            if (name.none { it.isLetterOrDigit() }) {
                errors.add("Group '$name': name must contain at least one letter or digit")
                return@forEachIndexed
            }
            val duplicateInPayload = payloadGroups.take(index).any { StringUtils.isEquivalentOnAlphanumerics(it.name, name) }
            if (duplicateInPayload) {
                errors.add("Group '$name': duplicates another group name in the same payload")
            }

            // Every org is bootstrapped with the SystemUnitGroup rows (see UnitGroupSeeder), so a payload
            // naming one of them (e.g. "Weight") always matches something already in the database. Treat
            // that as a reuse rather than a conflict, and skip further checks for this group entirely.
            if (existingGroups.any { it.systemDefined && StringUtils.isEquivalent(it.name, name) }) {
                return@forEachIndexed
            }

            if (existingGroups.any { StringUtils.isEquivalentOnAlphanumerics(it.name, name) }) {
                errors.add("Group '$name': name already exists in the database")
            }
        }
    }

    /** Maps every known unit value code - existing in the DB or declared in the payload - to its group name. */
    private fun buildGroupNameByCode(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingGroups: Collection<UnitGroupDto>
    ): Map<String, String> {
        val existingGroupNameById = existingGroups.associate { it.id to it.name }
        val fromExisting = existingUnitValues.mapNotNull { unitValue ->
            existingGroupNameById[unitValue.unitGroupId]?.let { unitValue.code to it }
        }
        val fromPayload = payloadGroups.flatMap { group ->
            group.unitValues.mapNotNull { unitValue ->
                StringUtils.getValueOrNull(unitValue.code)?.let { it to (StringUtils.getValueOrNull(group.name) ?: "") }
            }
        }
        return (fromExisting + fromPayload).toMap()
    }

    private fun validateUnitValues(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingGroups: Collection<UnitGroupDto>,
        groupNameByCode: Map<String, String>,
        errors: MutableList<String>
    ) {
        val allPayloadValues = payloadGroups.flatMap { group -> group.unitValues.map { group to it } }
        val existingCodes = existingUnitValues.map { it.code }.toSet()
        val existingSystemDefinedCodes = existingUnitValues.filter { it.systemDefined }.map { it.code }.toSet()
        val existingGroupNameById = existingGroups.associate { it.id to it.name }

        allPayloadValues.forEachIndexed { index, (group, unitValue) ->
            val label = StringUtils.getValueOrNull(unitValue.code) ?: StringUtils.getValueOrNull(unitValue.name) ?: "at position ${index + 1}"

            if (StringUtils.hasValue(unitValue.code) && allPayloadValues.take(index).any { it.second.code == unitValue.code }) {
                errors.add("Unit value '${unitValue.code}': duplicates another unit value code in the same payload")
            }

            if (unitValue.code != null && unitValue.code in existingSystemDefinedCodes) {
                return@forEachIndexed
            }

            if (!StringUtils.hasValue(unitValue.name)) {
                errors.add("Unit value $label: name is required")
            } else if (isReservedRootPieceName(group, unitValue.name!!)) {
                errors.add("Unit value '${unitValue.name}': the '${group.name}' group's root Piece is created automatically; reference it with baseUnitCode '$PIECE_BASE_UNIT_CODE'")
            } else if (isDuplicateUnitValueName(group, unitValue.name, existingUnitValues, existingGroupNameById, allPayloadValues.take(index))) {
                errors.add("Unit value '${unitValue.name}': name already exists in the '${group.name}' group")
            }

            if (!StringUtils.hasValue(unitValue.code)) {
                errors.add("Unit value $label: code is required")
            } else {
                val code = unitValue.code!!
                if (code.length > MAX_UNIT_VALUE_CODE_LENGTH) {
                    errors.add("Unit value '$code': code must be at most $MAX_UNIT_VALUE_CODE_LENGTH characters")
                } else if (existingCodes.contains(code)) {
                    errors.add("Unit value '$code': code already exists in the database")
                }
            }

            validateBaseUnitReference(group, unitValue, groupNameByCode, existingCodes, allPayloadValues, label, errors)
        }
    }

    private fun isReservedRootPieceName(group: UnitGroupBulkInsertDto, unitValueName: String): Boolean =
        !SystemUnitGroup.isExcludedFromPieceAutoInsert(group.name) &&
            StringUtils.isEquivalentOnAlphanumerics(unitValueName, SystemUnitValue.PIECE.unitName)

    private fun isDuplicateUnitValueName(
        group: UnitGroupBulkInsertDto,
        name: String?,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingGroupNameById: Map<UUID, String>,
        precedingPayloadValues: List<Pair<UnitGroupBulkInsertDto, UnitValueBulkInsertDto>>
    ): Boolean {
        val existingInSameGroup = existingUnitValues.any {
            StringUtils.isEquivalent(existingGroupNameById[it.unitGroupId], group.name) && StringUtils.isEquivalentOnAlphanumerics(it.name, name)
        }
        val payloadInSameGroup = precedingPayloadValues.any {
            StringUtils.isEquivalent(it.first.name, group.name) && StringUtils.isEquivalentOnAlphanumerics(it.second.name, name)
        }
        return existingInSameGroup || payloadInSameGroup
    }

    private fun validateBaseUnitReference(
        group: UnitGroupBulkInsertDto,
        unitValue: UnitValueBulkInsertDto,
        groupNameByCode: Map<String, String>,
        existingCodes: Set<String>,
        allPayloadValues: List<Pair<UnitGroupBulkInsertDto, UnitValueBulkInsertDto>>,
        label: String,
        errors: MutableList<String>
    ) {
        val baseUnitCode = unitValue.baseUnitCode
        val unitsOfBasePerUnit = unitValue.unitsOfBasePerUnit

        if (baseUnitCode != null && unitsOfBasePerUnit == null) {
            errors.add("Unit value $label: baseUnitCode requires unitsOfBasePerUnit")
        }
        if (unitsOfBasePerUnit != null && baseUnitCode == null) {
            errors.add("Unit value $label: unitsOfBasePerUnit requires a baseUnitCode")
        }
        if (unitsOfBasePerUnit != null && unitsOfBasePerUnit < 1) {
            errors.add("Unit value $label: unitsOfBasePerUnit must be a positive whole number")
        }
        if (unitsOfBasePerUnit != null && unitsOfBasePerUnit > UnitChainResolver.MAX_SIZE_IN_ROOT) {
            errors.add("Unit value $label: ${UnitChainResolver.CHAIN_SIZE_TOO_LARGE}")
        }
        if (isMiscellaneous(group.name)) {
            if (baseUnitCode != null || unitsOfBasePerUnit != null) {
                errors.add("Unit value $label: a Miscellaneous unit stands alone and cannot have a base unit")
            }
            return
        }
        if (baseUnitCode == null) {
            errors.add("Unit value $label: a unit in the '${group.name}' group needs a baseUnitCode, because the group's root unit already exists")
            return
        }
        if (baseUnitCode == PIECE_BASE_UNIT_CODE) {
            if (SystemUnitGroup.isExcludedFromPieceAutoInsert(group.name)) {
                errors.add("Unit value $label: baseUnitCode '$PIECE_BASE_UNIT_CODE' is not valid inside the '${group.name}' group")
            }
            return
        }

        val resolvesInPayload = allPayloadValues.any { it.second.code == baseUnitCode }
        if (!resolvesInPayload && baseUnitCode !in existingCodes) {
            errors.add("Unit value $label: baseUnitCode '$baseUnitCode' does not match any unit code in the payload or the database")
            return
        }
        val baseGroupName = groupNameByCode[baseUnitCode]
        if (baseGroupName != null && !StringUtils.isEquivalent(baseGroupName, group.name)) {
            errors.add("Unit value $label: baseUnitCode '$baseUnitCode' belongs to a different unit group")
        }
    }

    // Long group names can share a truncated Piece code slug, so the collision is reported here and not by the database
    private fun validateGeneratedPieceCodes(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingGroups: Collection<UnitGroupDto>,
        errors: MutableList<String>
    ) {
        val existingCodes = existingUnitValues.map { it.code }.toSet()
        val customGroups = payloadGroups.filter { group ->
            StringUtils.hasValue(group.name) &&
                !SystemUnitGroup.isExcludedFromPieceAutoInsert(group.name) &&
                existingGroups.none { it.systemDefined && StringUtils.isEquivalent(it.name, group.name) }
        }
        customGroups.forEachIndexed { index, group ->
            val pieceCode = SystemUnitValue.pieceCodeForGroup(group.name)
            val collidesWithPayloadGroup = customGroups.take(index).any { SystemUnitValue.pieceCodeForGroup(it.name) == pieceCode }
            if (collidesWithPayloadGroup || pieceCode in existingCodes) {
                errors.add("Group '${group.name}': its generated Piece code '$pieceCode' collides with another unit code; choose a name that differs earlier in the text")
            }
        }
    }

    private fun validateChainSizes(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        errors: MutableList<String>
    ) {
        val nodeIdByCode = (existingUnitValues.map { it.code } + payloadGroups.flatMap { group -> group.unitValues.mapNotNull { it.code } } +
            payloadGroups.map { SystemUnitValue.pieceCodeForGroup(it.name) })
            .distinct()
            .associateWith { UUID.randomUUID() }
        val existingNodes = existingUnitValues.map { unitValue ->
            val baseUnitCode = existingUnitValues.find { it.id == unitValue.baseUnit }?.code
            UnitChainNode(nodeIdByCode.getValue(unitValue.code), baseUnitCode?.let { nodeIdByCode[it] }, unitValue.unitsOfBasePerUnit)
        }
        val payloadNodes = payloadGroups.flatMap { group ->
            val pieceCode = SystemUnitValue.pieceCodeForGroup(group.name)
            val rootNode = UnitChainNode(nodeIdByCode.getValue(pieceCode), null, null)
            val unitNodes = group.unitValues.filter { it.code !in existingUnitValues.map { existing -> existing.code } }.map { unitValue ->
                val baseUnitCode = if (unitValue.baseUnitCode == PIECE_BASE_UNIT_CODE) pieceCode else unitValue.baseUnitCode
                UnitChainNode(nodeIdByCode.getValue(unitValue.code!!), baseUnitCode?.let { nodeIdByCode[it] }, unitValue.unitsOfBasePerUnit)
            }
            if (SystemUnitGroup.isExcludedFromPieceAutoInsert(group.name) || rootNode.id in existingNodes.map { it.id }) unitNodes else listOf(rootNode) + unitNodes
        }
        try {
            UnitChainResolver.resolvePlacements(existingNodes + payloadNodes)
        } catch (chainException: RtsGenericException) {
            errors.add("Unit chain rejected: ${chainException.message}")
        }
    }

    private fun validateNoCycles(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        groupNameByCode: Map<String, String>,
        errors: MutableList<String>
    ) {
        val graph = UnitValueDependencyGraph()

        existingUnitValues.filter { !isMiscellaneous(groupNameByCode[it.code]) }.forEach { unitValue ->
            graph.addNode(unitValue.code)
            val baseUnitCode = existingUnitValues.find { candidate -> candidate.id == unitValue.baseUnit }?.code
            if (baseUnitCode != null) graph.addEdge(baseUnitCode, unitValue.code)
        }

        payloadGroups.filter { !isMiscellaneous(it.name) }.forEach { group ->
            group.unitValues.forEach { unitValue ->
                val code = StringUtils.getValueOrNull(unitValue.code) ?: return@forEach
                graph.addNode(code)
                val baseUnitCode = unitValue.baseUnitCode
                if (baseUnitCode != null && baseUnitCode != PIECE_BASE_UNIT_CODE) {
                    graph.addEdge(baseUnitCode, code)
                }
            }
        }

        graph.findCycle()?.let { cycle -> errors.add("Cycle detected: ${cycle.joinToString(" -> ")}") }
    }

    private fun isMiscellaneous(groupName: String?): Boolean =
        StringUtils.isEquivalent(groupName, SystemUnitGroup.MISC.groupName)

    companion object {
        const val PIECE_BASE_UNIT_CODE = "__piece__"
        const val MAX_UNIT_VALUE_CODE_LENGTH = 25
    }
}
