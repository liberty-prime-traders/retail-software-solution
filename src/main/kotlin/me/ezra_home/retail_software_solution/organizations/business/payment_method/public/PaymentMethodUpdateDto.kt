package me.ezra_home.retail_software_solution.organizations.business.payment_method.public

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class PaymentMethodUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
): Serializable
