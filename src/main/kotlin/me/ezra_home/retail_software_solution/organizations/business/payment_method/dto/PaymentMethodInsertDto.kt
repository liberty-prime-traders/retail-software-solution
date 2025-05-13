package me.ezra_home.retail_software_solution.organizations.business.payment_method.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.PaymentMethodEntity}
 */
data class PaymentMethodInsertDto (
    val name: String? = null,
    val description: String? = null
): Serializable
