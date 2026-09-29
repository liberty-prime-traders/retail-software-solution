package me.ezra_home.retail_software_solution.platform.business.authorization_pass.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PassType(override val code: String) : HasCode {
    CREATE_ORGANIZATION("CRORG")
}
