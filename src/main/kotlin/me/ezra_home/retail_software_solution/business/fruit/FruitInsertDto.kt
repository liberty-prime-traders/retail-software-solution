package me.ezra_home.retail_software_solution.business.fruit

import java.io.Serializable
import java.math.BigDecimal

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitInsertDto(
    val name: String? = null,
    val alternateName: String? = null,
    val color: String? = null,
    val cost: BigDecimal? = null,
    val edibleInd: Boolean? = null
) : Serializable