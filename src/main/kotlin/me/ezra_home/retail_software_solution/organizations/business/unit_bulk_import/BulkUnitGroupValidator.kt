package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.RootUnitDefinition
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.SystemUnitValue
import me.ezra_home.retail_software_solution.util.business.StringUtils

object BulkUnitGroupValidator {

    fun validateGroupNames(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingGroups: Collection<UnitGroupDto>,
        errors: MutableList<String>
    ) {
        val seenPayloadNameKeys = HashSet<String>()
        val systemGroupKeys = existingGroups.filter { it.systemDefined }.map { BulkUnitNameKeys.groupKey(it.name) }.toSet()
        val existingNameKeys = existingGroups.mapNotNull { BulkUnitNameKeys.alphanumericNameKey(it.name) }.toSet()
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
            val nameKey = BulkUnitNameKeys.alphanumericNameKey(name)
            if (nameKey != null && !seenPayloadNameKeys.add(nameKey)) {
                errors.add("Group '$name': duplicates another group name in the same payload")
            }

            // Every org is bootstrapped with the SystemUnitGroup rows (see UnitGroupSeeder), so a payload
            // naming one of them (e.g. "Weight") always matches something already in the database. Treat
            // that as a reuse rather than a conflict, and skip further checks for this group entirely.
            if (BulkUnitNameKeys.groupKey(name) in systemGroupKeys) {
                return@forEachIndexed
            }

            if (nameKey != null && nameKey in existingNameKeys) {
                errors.add("Group '$name': name already exists in the database")
            }
        }
    }

    fun validateBaseUnitNames(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingGroups: Collection<UnitGroupDto>,
        errors: MutableList<String>
    ) {
        payloadGroups.filter { StringUtils.hasValue(it.name) && StringUtils.hasValue(it.baseUnitName) }.forEach { group ->
            if (reusesSystemGroup(group, existingGroups)) {
                errors.add(systemGroupAlreadyHasRootUnit(group, "baseUnitName"))
            } else if (StringUtils.alphanumericKey(group.baseUnitName!!).isEmpty()) {
                errors.add("Group '${group.name}': baseUnitName must contain at least one letter or digit")
            }
        }
    }

    fun validateBaseUnitCodes(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingCodes: Set<String>,
        existingGroups: Collection<UnitGroupDto>,
        errors: MutableList<String>
    ) {
        val payloadUnitCodes = payloadGroups.flatMap { it.unitValues }.mapNotNull { StringUtils.getValueOrNull(it.code) }.toSet()
        val namedGroups = payloadGroups.filter { StringUtils.hasValue(it.name) }

        namedGroups.filter { StringUtils.hasValue(it.baseUnitCode) && reusesSystemGroup(it, existingGroups) }.forEach { group ->
            errors.add(systemGroupAlreadyHasRootUnit(group, "baseUnitCode"))
        }

        val customGroups = namedGroups.filter { !reusesSystemGroup(it, existingGroups) }
        val seenRootCodes = HashSet<String>()
        customGroups.forEach { group ->
            val suppliedCode = StringUtils.getValueOrNull(group.baseUnitCode)
            val rootCode = RootUnitDefinition.effectiveBaseUnitCode(group.name, suppliedCode)
            val collidesWithPayloadGroup = rootCode in seenRootCodes
            seenRootCodes.add(RootUnitDefinition.effectiveBaseUnitCode(group.name, group.baseUnitCode))
            if (suppliedCode == null) {
                if (collidesWithPayloadGroup || rootCode in existingCodes || rootCode in payloadUnitCodes) {
                    errors.add(
                        "Group '${group.name}': its generated base unit code '$rootCode' " +
                            "collides with another unit code; choose a name that differs earlier in the text, or supply a baseUnitCode"
                    )
                }
                return@forEach
            }
            when {
                suppliedCode.length > SystemUnitValue.MAX_CODE_LENGTH ->
                    errors.add("Group '${group.name}': baseUnitCode must be at most ${SystemUnitValue.MAX_CODE_LENGTH} characters")
                suppliedCode == RootUnitDefinition.PIECE_BASE_UNIT_CODE ->
                    errors.add("Group '${group.name}': baseUnitCode '$suppliedCode' is reserved for referencing a group's root unit")
                suppliedCode in payloadUnitCodes ->
                    errors.add("Group '${group.name}': baseUnitCode '$suppliedCode' duplicates a unit value code in the same payload")
                collidesWithPayloadGroup ->
                    errors.add("Group '${group.name}': baseUnitCode '$suppliedCode' duplicates another group's base unit code")
                suppliedCode in existingCodes ->
                    errors.add("Group '${group.name}': baseUnitCode '$suppliedCode' already exists in the database")
            }
        }
    }

    fun explicitRootCodeToGroupName(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingGroups: Collection<UnitGroupDto>
    ): Map<String, String> = payloadGroups
        .filter { StringUtils.hasValue(it.name) && StringUtils.hasValue(it.baseUnitCode) && !reusesSystemGroup(it, existingGroups) }
        .associate { StringUtils.getValueOrNull(it.baseUnitCode)!! to it.name!! }

    private fun systemGroupAlreadyHasRootUnit(group: UnitGroupBulkInsertDto, fieldName: String) =
        "Group '${group.name}': $fieldName is not allowed on a system-defined group, which already has its root unit"

    private fun reusesSystemGroup(group: UnitGroupBulkInsertDto, existingGroups: Collection<UnitGroupDto>): Boolean =
        SystemUnitGroup.isExcludedFromRootAutoInsert(group.name) ||
            existingGroups.any { it.systemDefined && StringUtils.isEquivalent(it.name, group.name) }
}
