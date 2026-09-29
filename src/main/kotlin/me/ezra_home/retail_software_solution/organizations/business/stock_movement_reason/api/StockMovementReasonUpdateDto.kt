package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.util.Optional
import java.util.UUID

data class StockMovementReasonUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) {
    fun applyTo(existing: StockMovementReasonDto): StockMovementReasonDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = StringUtils.useIfProvided(description, existing.description)
    )
}
