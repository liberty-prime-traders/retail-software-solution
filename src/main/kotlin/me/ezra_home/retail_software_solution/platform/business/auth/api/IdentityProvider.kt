package me.ezra_home.retail_software_solution.platform.business.auth.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class IdentityProvider(override val code: String) : HasCode {
    GOOGLE("GGL")
}
