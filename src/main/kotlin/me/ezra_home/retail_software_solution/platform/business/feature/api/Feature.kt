package me.ezra_home.retail_software_solution.platform.business.feature.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class Feature(override val code: String) : HasCode {
    CHART_OF_ACCOUNTS("CHART_OF_ACCOUNTS"),
    TAX_CONFIGURATION("TAX_CONFIGURATION");
}
