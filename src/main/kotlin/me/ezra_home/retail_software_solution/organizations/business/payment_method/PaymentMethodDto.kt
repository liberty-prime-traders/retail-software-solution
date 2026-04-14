package me.ezra_home.retail_software_solution.organizations.business.payment_method

import java.time.OffsetDateTime
import java.util.UUID

data class PaymentMethodDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val name: String? = null,
    val description: String? = null,
    val accountCode: String? = null
)
