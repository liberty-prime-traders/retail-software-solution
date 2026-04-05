package me.ezra_home.retail_software_solution.util.business.mappers

import org.springframework.stereotype.Component
import java.util.Optional

@Component
object OptionalQualifier {
    fun <T, EXTENDER: T> fromOptional(optional: Optional<EXTENDER>): T? = optional.orElse(null)
}
