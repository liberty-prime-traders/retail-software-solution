package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.BulkUnitImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitValueBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.RootUnitDefinition
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.SystemUnitValue
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitChainResolver
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component


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

        BulkUnitGroupValidator.validateGroupNames(payloadGroups, existingGroups, errors)
        BulkUnitGroupValidator.validateBaseUnitNames(payloadGroups, existingGroups, errors)
        val existingUnitCodes = ExistingUnitCodes(existingUnitValues)
        BulkUnitGroupValidator.validateBaseUnitCodes(payloadGroups, existingUnitCodes.codes, existingGroups, errors)

        val groupNameByCode = buildGroupNameByCode(payloadGroups, existingUnitValues, existingGroups)
        val explicitRootCodeToGroupName = BulkUnitGroupValidator.explicitRootCodeToGroupName(payloadGroups, existingGroups)
        validateUnitValues(payloadGroups, existingUnitValues, existingUnitCodes, existingGroups, groupNameByCode, explicitRootCodeToGroupName, errors)
        BulkUnitChainValidator.validateNoCycles(payloadGroups, existingUnitValues, existingUnitCodes, groupNameByCode, errors)
        if (errors.isEmpty()) {
            BulkUnitChainValidator.validateChainSizes(payloadGroups, existingUnitValues, existingUnitCodes, errors)
        }

        return errors
    }

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
        return (fromPayload + fromExisting).toMap()
    }

    private fun validateUnitValues(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingUnitCodes: ExistingUnitCodes,
        existingGroups: Collection<UnitGroupDto>,
        groupNameByCode: Map<String, String>,
        explicitRootCodeToGroupName: Map<String, String>,
        errors: MutableList<String>
    ) {
        val allPayloadValues = payloadGroups.flatMap { group -> group.unitValues.map { group to it } }
        val payloadCodes = allPayloadValues.mapNotNull { it.second.code }.toSet()
        val existingCodes = existingUnitCodes.codes
        val existingSystemDefinedCodes = existingUnitCodes.systemDefinedCodes
        val existingGroupNameById = existingGroups.associate { it.id to it.name }
        val existingNameKeysByGroupKey = existingUnitValues
            .mapNotNull { unitValue ->
                BulkUnitNameKeys.alphanumericNameKey(unitValue.name)?.let { nameKey ->
                    BulkUnitNameKeys.groupKey(existingGroupNameById[unitValue.unitGroupId]) to nameKey
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
        val precedingPayloadValues = PrecedingPayloadValues()

        allPayloadValues.forEachIndexed { index, (group, unitValue) ->
            val label = StringUtils.getValueOrNull(unitValue.code) ?: StringUtils.getValueOrNull(unitValue.name) ?: "at position ${index + 1}"

            if (StringUtils.hasValue(unitValue.code) && precedingPayloadValues.hasCode(unitValue.code!!)) {
                errors.add("Unit value '${unitValue.code}': duplicates another unit value code in the same payload")
            }

            if (unitValue.code != null && unitValue.code in existingSystemDefinedCodes) {
                precedingPayloadValues.register(group, unitValue)
                val owningGroupName = groupNameByCode[unitValue.code]
                if (owningGroupName != null && !StringUtils.isEquivalent(owningGroupName, group.name)) {
                    errors.add("Unit value '${unitValue.code}': a system unit belongs to the '$owningGroupName' group, not '${group.name}'")
                }
                return@forEachIndexed
            }

            if (!StringUtils.hasValue(unitValue.name)) {
                errors.add("Unit value $label: name is required")
            } else if (isReservedRootUnitName(group, unitValue.name!!)) {
                errors.add("Unit value '${unitValue.name}': the '${group.name}' group's root unit '${RootUnitDefinition.effectiveBaseUnitName(group.baseUnitName)}' is created automatically; reference it with baseUnitCode '${RootUnitDefinition.PIECE_BASE_UNIT_CODE}'")
            } else if (isDuplicateUnitValueName(group, unitValue.name, existingNameKeysByGroupKey, precedingPayloadValues)) {
                errors.add("Unit value '${unitValue.name}': name already exists in the '${group.name}' group")
            }

            if (!StringUtils.hasValue(unitValue.code)) {
                errors.add("Unit value $label: code is required")
            } else {
                val code = unitValue.code!!
                if (code == RootUnitDefinition.PIECE_BASE_UNIT_CODE) {
                    errors.add("Unit value '$code': code is reserved")
                } else if (code.length > SystemUnitValue.MAX_CODE_LENGTH) {
                    errors.add("Unit value '$code': code must be at most ${SystemUnitValue.MAX_CODE_LENGTH} characters")
                } else if (existingCodes.contains(code)) {
                    errors.add("Unit value '$code': code already exists in the database")
                }
            }

            validateBaseUnitReference(group, unitValue, groupNameByCode, explicitRootCodeToGroupName, existingCodes, payloadCodes, label, errors)
            precedingPayloadValues.register(group, unitValue)
        }
    }

    private fun isReservedRootUnitName(group: UnitGroupBulkInsertDto, unitValueName: String): Boolean =
        !SystemUnitGroup.isExcludedFromRootAutoInsert(group.name) &&
            StringUtils.isEquivalentOnAlphanumerics(unitValueName, RootUnitDefinition.effectiveBaseUnitName(group.baseUnitName))

    private fun isDuplicateUnitValueName(
        group: UnitGroupBulkInsertDto,
        name: String?,
        existingNameKeysByGroupKey: Map<String, Set<String>>,
        precedingPayloadValues: PrecedingPayloadValues
    ): Boolean {
        val nameKey = BulkUnitNameKeys.alphanumericNameKey(name) ?: return false
        val groupKey = BulkUnitNameKeys.groupKey(group.name)
        return nameKey in existingNameKeysByGroupKey[groupKey].orEmpty() || precedingPayloadValues.hasName(groupKey, nameKey)
    }

    private class PrecedingPayloadValues {
        private val codes = HashSet<String>()
        private val nameKeysByGroupKey = HashMap<String, MutableSet<String>>()

        fun hasCode(code: String): Boolean = code in codes

        fun hasName(groupKey: String, nameKey: String): Boolean = nameKey in nameKeysByGroupKey[groupKey].orEmpty()

        fun register(group: UnitGroupBulkInsertDto, unitValue: UnitValueBulkInsertDto) {
            unitValue.code?.let { codes.add(it) }
            BulkUnitNameKeys.alphanumericNameKey(unitValue.name)?.let { nameKey ->
                nameKeysByGroupKey.getOrPut(BulkUnitNameKeys.groupKey(group.name)) { HashSet() }.add(nameKey)
            }
        }
    }

    private fun validateBaseUnitReference(
        group: UnitGroupBulkInsertDto,
        unitValue: UnitValueBulkInsertDto,
        groupNameByCode: Map<String, String>,
        explicitRootCodeToGroupName: Map<String, String>,
        existingCodes: Set<String>,
        payloadCodes: Set<String>,
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
        if (SystemUnitGroup.isMiscellaneous(group.name)) {
            if (baseUnitCode != null || unitsOfBasePerUnit != null) {
                errors.add("Unit value $label: a Miscellaneous unit stands alone and cannot have a base unit")
            }
            return
        }
        if (baseUnitCode == null) {
            errors.add("Unit value $label: a unit in the '${group.name}' group needs a baseUnitCode, because the group's root unit already exists")
            return
        }
        if (baseUnitCode == RootUnitDefinition.PIECE_BASE_UNIT_CODE) {
            if (SystemUnitGroup.isExcludedFromRootAutoInsert(group.name)) {
                errors.add("Unit value $label: baseUnitCode '${RootUnitDefinition.PIECE_BASE_UNIT_CODE}' is not valid inside the '${group.name}' group")
            }
            return
        }

        val explicitRootGroupName = explicitRootCodeToGroupName[baseUnitCode]
        if (explicitRootGroupName != null) {
            if (!StringUtils.isEquivalent(explicitRootGroupName, group.name)) {
                errors.add("Unit value $label: baseUnitCode '$baseUnitCode' belongs to a different unit group")
            }
            return
        }

        if (baseUnitCode !in payloadCodes && baseUnitCode !in existingCodes) {
            errors.add("Unit value $label: baseUnitCode '$baseUnitCode' does not match any unit code in the payload or the database")
            return
        }
        val baseGroupName = groupNameByCode[baseUnitCode]
        if (baseGroupName != null && !StringUtils.isEquivalent(baseGroupName, group.name)) {
            errors.add("Unit value $label: baseUnitCode '$baseUnitCode' belongs to a different unit group")
        }
    }
}
