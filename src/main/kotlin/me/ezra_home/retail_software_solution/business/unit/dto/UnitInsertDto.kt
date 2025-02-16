package me.ezra_home.retail_software_solution.business.unit.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitEntity}
 */
data class UnitInsertDto(
    val dataType: String? = null,
    val name: String? = null,
    val code: String? = null,
    val decimalCount: Short? = null,
    val enumerated: Boolean? = null,
    val enumerationOptions: String? = null,
) : Serializable