package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class TaxDirection(override val code: String) : HasCode {
    OUTPUT("O"),
    INPUT("I")
}
