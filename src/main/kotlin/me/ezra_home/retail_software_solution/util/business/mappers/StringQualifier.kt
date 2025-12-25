package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component
import java.util.Optional

@Component
object StringQualifier {
    fun mapString(value: String?): String? {
        return StringUtils.getValueOrNull(value)
    }

    fun mapString(value: Optional<String>): String? {
        return mapString(OptionalQualifier.fromOptional(value))
    }
}
