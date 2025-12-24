package me.ezra_home.retail_software_solution.util.business.mappers

import org.springframework.stereotype.Component
import java.util.Optional

@Component
object OptionalQualifier {
    fun <T> fromOptional(optional: Optional<T>): T? = optional.orElse(null)
}
