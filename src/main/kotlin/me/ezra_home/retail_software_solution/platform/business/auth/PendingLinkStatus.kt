package me.ezra_home.retail_software_solution.platform.business.auth

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PendingLinkStatus(override val code: String) : HasCode {
    PENDING("P"),
    CONSUMED("C"),
    INVALIDATED("I")
}
