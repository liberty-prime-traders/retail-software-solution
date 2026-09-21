package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemUnitGroup(override val code: String, val groupName: String, val description: String) : HasCode {
    MISC("MSC", "Miscellaneous", "General purpose units that do not fit other categories"),
    WEIGHT("WGT", "Weight", "Units of mass or weight"),
    VOLUME("VLM", "Volume", "Units of liquid or dry volume"),
    COUNTABLE("CNT", "Count", "Units based on discrete countable items");

    companion object {
        private val EXCLUDED_FROM_PIECE_AUTO_INSERT = setOf(MISC, WEIGHT, VOLUME)

        fun isExcludedFromPieceAutoInsert(groupName: String?): Boolean =
            EXCLUDED_FROM_PIECE_AUTO_INSERT.any { StringUtils.isEquivalent(it.groupName, groupName) }
    }
}
