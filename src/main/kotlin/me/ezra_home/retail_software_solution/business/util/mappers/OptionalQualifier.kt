package me.ezra_home.retail_software_solution.business.util.mappers

import org.springframework.stereotype.Component
import java.util.Optional

@Component
class OptionalQualifier {
    fun <T> fromOptional(optional: Optional<T>): T? = optional.get()
}
