package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.BulkUnitImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitConversionBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitValueBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionService
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class BulkUnitImportValidator(
    private val unitGroupService: UnitGroupService,
    private val unitValueFetcher: UnitValueFetcher,
    private val unitConversionService: UnitConversionService
) {

    fun validate(bulkUnitImportRequestDto: BulkUnitImportRequestDto): List<String> {
        val errors = mutableListOf<String>()
        val existingGroups = unitGroupService.getAllUnitGroupDtos()
        val existingUnitValues = unitValueFetcher.getAllUnitValues()
        val existingConversions = unitConversionService.getAll()
        val payloadGroups = bulkUnitImportRequestDto.unitGroups

        validateGroupNames(payloadGroups, existingGroups, errors)

        val groupNameByCode = buildGroupNameByCode(payloadGroups, existingUnitValues, existingGroups)
        validateUnitValues(payloadGroups, existingUnitValues, existingGroups, groupNameByCode, errors)
        validateConversions(bulkUnitImportRequestDto.unitConversions, groupNameByCode, existingUnitValues, existingConversions, errors)
        validateNoCycles(payloadGroups, bulkUnitImportRequestDto.unitConversions, existingUnitValues, groupNameByCode, errors)

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
            val duplicateInPayload = payloadGroups.take(index).any { StringUtils.isEquivalent(it.name, name) }
            if (duplicateInPayload) {
                errors.add("Group '$name': duplicates another group name in the same payload")
            }
            if (existingGroups.any { StringUtils.isEquivalent(it.name, name) }) {
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
        val existingGroupNameById = existingGroups.associate { it.id to it.name }

        allPayloadValues.forEachIndexed { index, (group, unitValue) ->
            val label = StringUtils.getValueOrNull(unitValue.code) ?: StringUtils.getValueOrNull(unitValue.name) ?: "at position ${index + 1}"

            if (!StringUtils.hasValue(unitValue.name)) {
                errors.add("Unit value $label: name is required")
            } else if (isDuplicateUnitValueName(group, unitValue.name, existingUnitValues, existingGroupNameById, allPayloadValues.take(index))) {
                errors.add("Unit value '${unitValue.name}': name already exists in the '${group.name}' group")
            }

            if (!StringUtils.hasValue(unitValue.code)) {
                errors.add("Unit value $label: code is required")
            } else {
                val code = unitValue.code!!
                if (code.length > MAX_UNIT_VALUE_CODE_LENGTH) {
                    errors.add("Unit value '$code': code must be at most $MAX_UNIT_VALUE_CODE_LENGTH characters")
                } else if (allPayloadValues.take(index).any { it.second.code == code }) {
                    errors.add("Unit value '$code': duplicates another unit value code in the same payload")
                } else if (existingCodes.contains(code)) {
                    errors.add("Unit value '$code': code already exists in the database")
                }
            }

            validateBaseUnitReference(group, unitValue, groupNameByCode, existingCodes, allPayloadValues, label, errors)
        }
    }

    private fun isDuplicateUnitValueName(
        group: UnitGroupBulkInsertDto,
        name: String?,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingGroupNameById: Map<UUID, String>,
        precedingPayloadValues: List<Pair<UnitGroupBulkInsertDto, UnitValueBulkInsertDto>>
    ): Boolean {
        val existingInSameGroup = existingUnitValues.any {
            StringUtils.isEquivalent(existingGroupNameById[it.unitGroupId], group.name) && StringUtils.isEquivalent(it.name, name)
        }
        val payloadInSameGroup = precedingPayloadValues.any {
            StringUtils.isEquivalent(it.first.name, group.name) && StringUtils.isEquivalent(it.second.name, name)
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
        if (unitsOfBasePerUnit != null && unitsOfBasePerUnit <= 0.0) {
            errors.add("Unit value $label: unitsOfBasePerUnit must be greater than zero")
        }
        if (baseUnitCode == null) {
            return
        }
        if (baseUnitCode == PIECE_BASE_UNIT_CODE) {
            if (isExcludedFromPieceAutoInsert(group.name)) {
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

    private fun validateConversions(
        conversions: List<UnitConversionBulkInsertDto>,
        groupNameByCode: Map<String, String>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingConversions: List<UnitConversionDto>,
        errors: MutableList<String>
    ) {
        val codeById = existingUnitValues.associate { it.id to it.code }
        val existingPairs = existingConversions.mapNotNull { conversion ->
            val fromCode = codeById[conversion.fromUnitId]
            val toCode = codeById[conversion.toUnitId]
            if (fromCode != null && toCode != null) fromCode to toCode else null
        }

        conversions.forEachIndexed { index, conversion ->
            val fromLabel = StringUtils.getValueOrNull(conversion.fromUnitCode) ?: "?"
            val toLabel = StringUtils.getValueOrNull(conversion.toUnitCode) ?: "?"
            val label = "'$fromLabel' -> '$toLabel'"

            if (!StringUtils.hasValue(conversion.fromUnitCode) || !StringUtils.hasValue(conversion.toUnitCode)) {
                errors.add("Conversion $label: fromUnitCode and toUnitCode are required")
                return@forEachIndexed
            }
            if (conversion.fromUnitCode == conversion.toUnitCode) {
                errors.add("Conversion $label: fromUnitCode and toUnitCode must be different")
            }
            if (conversion.unitsOfBasePerUnit == null || conversion.unitsOfBasePerUnit <= BigDecimal.ZERO) {
                errors.add("Conversion $label: unitsOfBasePerUnit is required and must be greater than zero")
            }
            if (conversion.fromUnitCode !in groupNameByCode) {
                errors.add("Conversion $label: fromUnitCode '${conversion.fromUnitCode}' does not match any unit code in the payload or the database")
            }
            if (conversion.toUnitCode !in groupNameByCode) {
                errors.add("Conversion $label: toUnitCode '${conversion.toUnitCode}' does not match any unit code in the payload or the database")
            }

            val duplicateInPayload = conversions.take(index).any {
                isSamePair(it.fromUnitCode, it.toUnitCode, conversion.fromUnitCode, conversion.toUnitCode)
            }
            if (duplicateInPayload) {
                errors.add("Conversion $label: duplicates another conversion in the same payload")
            }
            val duplicateInDatabase = existingPairs.any {
                isSamePair(it.first, it.second, conversion.fromUnitCode, conversion.toUnitCode)
            }
            if (duplicateInDatabase) {
                errors.add("Conversion $label: a conversion between these two units already exists in the database")
            }

            val fromGroup = groupNameByCode[conversion.fromUnitCode]
            val toGroup = groupNameByCode[conversion.toUnitCode]
            if (fromGroup != null && toGroup != null && StringUtils.isEquivalent(fromGroup, toGroup)) {
                errors.add("Conversion $label: units in the same group are already connected via the base unit chain")
            }
        }
    }

    private fun isSamePair(fromA: String?, toA: String?, fromB: String?, toB: String?): Boolean =
        (fromA == fromB && toA == toB) || (fromA == toB && toA == fromB)

    private fun validateNoCycles(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        conversions: List<UnitConversionBulkInsertDto>,
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

        conversions.forEach { conversion ->
            val fromCode = StringUtils.getValueOrNull(conversion.fromUnitCode)
            val toCode = StringUtils.getValueOrNull(conversion.toUnitCode)
            if (fromCode != null && toCode != null) {
                graph.addEdge(fromCode, toCode)
            }
        }

        graph.findCycle()?.let { cycle -> errors.add("Cycle detected: ${cycle.joinToString(" -> ")}") }
    }

    private fun isMiscellaneous(groupName: String?): Boolean =
        StringUtils.isEquivalent(groupName, SystemUnitGroup.MISC.groupName)

    private fun isExcludedFromPieceAutoInsert(groupName: String?): Boolean =
        listOf(SystemUnitGroup.MISC, SystemUnitGroup.WEIGHT, SystemUnitGroup.VOLUME)
            .any { StringUtils.isEquivalent(groupName, it.groupName) }

    companion object {
        const val PIECE_BASE_UNIT_CODE = "__piece__"
        const val MAX_UNIT_VALUE_CODE_LENGTH = 25
    }
}
