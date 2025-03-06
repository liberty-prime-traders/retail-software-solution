package me.ezra_home.retail_software_solution.model.dto

import java.math.BigDecimal
import java.util.UUID
import java.time.OffsetDateTime

data class FruitResponseDTO(
    var id: UUID,
    var name: String,
    var alternateName: String?,
    var color: String,
    var cost: BigDecimal,
    var edible: Boolean,
    var createdById: UUID?,
    var createdOn: OffsetDateTime?,
    var predecessorOfId: UUID?,
    var usageCount: Long
)
