package me.ezra_home.retail_software_solution.business.unit.dto

import me.ezra_home.retail_software_solution.model.enums.DataType
import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitEntity}
 */
data class UnitInsertDto(
    val dataType: DataType,
    val name: String,
    val code: String,
    val decimalCount: Short? = null,
    val enumerated: Boolean? = null,
    val enumerationOptions: List<String>? = null,
) : Serializable