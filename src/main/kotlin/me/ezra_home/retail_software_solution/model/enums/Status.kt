package me.ezra_home.retail_software_solution.model.enums

import lombok.Getter
import me.ezra_home.retail_software_solution.model.util.HasCode

@Getter
enum class Status(override val code: String) : HasCode {
    ACTIVE("A"),
    STOPPED("S"),

    OPENED("OP"),
    PENDING_OPEN("POP"),
    CLOSED("C");

}
