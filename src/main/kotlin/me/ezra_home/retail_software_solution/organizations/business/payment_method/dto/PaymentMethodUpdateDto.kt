package me.ezra_home.retail_software_solution.organizations.business.payment_method.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.PaymentMethodEntity}
 */
data class PaymentMethodUpdateDto (
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
): Serializable
