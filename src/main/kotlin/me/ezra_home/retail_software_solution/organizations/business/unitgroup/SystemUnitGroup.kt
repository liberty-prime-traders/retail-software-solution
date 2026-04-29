package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemUnitGroup(override val code: String, val groupName: String, val description: String) : HasCode {
    MISC("MSC", "Miscellaneous", "General purpose units that do not fit other categories"),
    WEIGHT("WGT", "Weight", "Units of mass or weight"),
    VOLUME("VLM", "Volume", "Units of liquid or dry volume"),
    COUNTABLE("CNT", "Count", "Units based on discrete countable items")
}
