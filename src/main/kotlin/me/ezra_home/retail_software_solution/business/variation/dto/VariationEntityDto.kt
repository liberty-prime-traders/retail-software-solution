package me.ezra_home.retail_software_solution.business.variation.dto

import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

/**
 * DTO for {@link me.ezra_home.retail_software_solution.business.variation.VariationEntity}
 */
data class VariationEntityDto(
    val variationId: UUID = UUID.randomUUID(),
    val name: String? = null,
    val description: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val createdBy: UUID? = null,
    val updatedBy: UUID? = null,
    val isActive: Boolean = true
) : Serializable