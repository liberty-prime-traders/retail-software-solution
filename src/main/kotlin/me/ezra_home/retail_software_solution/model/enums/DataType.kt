package me.ezra_home.retail_software_solution.model.enums

import me.ezra_home.retail_software_solution.model.util.HasCode

enum class DataType(override val code: String) : HasCode {
    TEXT("TXT"),
    NUMERIC("DCM")
}