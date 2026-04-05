package me.ezra_home.retail_software_solution.organizations.business.payment_method.public

import java.io.Serializable

data class PaymentMethodInsertDto(
    val name: String? = null,
    val description: String? = null
): Serializable
