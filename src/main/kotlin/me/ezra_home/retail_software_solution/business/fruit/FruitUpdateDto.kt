package me.ezra_home.retail_software_solution.business.fruit
import java.io.Serializable
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val alternateName: Optional<String>? = null,
    val color: Optional<String>? = null,
    val cost: Optional<BigDecimal>? = null,
    val edibleInd: Optional<Boolean>? = null
) : Serializable