package me.ezra_home.retail_software_solution.util.enums

import lombok.Getter

@Getter
enum class Status(override val code: String) : HasCode {
    ACTIVE("A"),
    STOPPED("S"),

    USED("USD"),
    UNUSED("UNSD"),
    ABANDONED("ABND");
}
