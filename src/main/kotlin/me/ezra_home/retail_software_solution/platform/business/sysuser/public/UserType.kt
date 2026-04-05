package me.ezra_home.retail_software_solution.platform.business.sysuser.public

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class UserType(override val code: String) : HasCode {
    END_USER("E"),
    SERVICE_ACCOUNT("S")
}
