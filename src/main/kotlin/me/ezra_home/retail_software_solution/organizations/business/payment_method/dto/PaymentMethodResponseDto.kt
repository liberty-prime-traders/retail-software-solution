package me.ezra_home.retail_software_solution.organizations.business.payment_method.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.PaymentMethodEntity}
 */
data class PaymentMethodResponseDto (
    val id: UUID?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?,
    val name: String?,
    val description: String?
): Serializable
