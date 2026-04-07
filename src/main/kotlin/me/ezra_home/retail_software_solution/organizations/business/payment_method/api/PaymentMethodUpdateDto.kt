package me.ezra_home.retail_software_solution.organizations.business.payment_method.api

import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodDto
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class PaymentMethodUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
): Serializable {

    fun applyTo(existing: PaymentMethodDto): PaymentMethodDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = description?.orElse(existing.description) ?: existing.description
    )
}
