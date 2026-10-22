package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.util.UUID

data class RootUnitDefinition(
    val unitGroupId: UUID,
    val name: String,
    val code: String,
    val description: String,
    val systemDefined: Boolean
) {

    companion object {
        const val DESCRIPTION = "Base unit representing a single item"
        const val PIECE_BASE_UNIT_CODE = "__piece__"

        fun effectiveBaseUnitName(baseUnitName: String?): String =
            StringUtils.getValueOrNull(baseUnitName) ?: SystemUnitValue.PIECE.unitName

        fun effectiveBaseUnitCode(unitGroupName: String?, baseUnitCode: String?): String =
            StringUtils.getValueOrNull(baseUnitCode) ?: SystemUnitValue.baseUnitCodeForGroup(unitGroupName)

        fun forGroup(unitGroupId: UUID, unitGroupName: String?, baseUnitName: String?, baseUnitCode: String?) = RootUnitDefinition(
            unitGroupId = unitGroupId,
            name = effectiveBaseUnitName(baseUnitName),
            code = effectiveBaseUnitCode(unitGroupName, baseUnitCode),
            description = DESCRIPTION,
            systemDefined = true
        )
    }
}
