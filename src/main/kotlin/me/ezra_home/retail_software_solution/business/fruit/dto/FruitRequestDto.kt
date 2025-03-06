package me.ezra_home.retail_software_solution.model.dto

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class FruitRequestDTO(

    @field:NotBlank(message = "Name cannot be blank")
    var name: String,

    var alternateName: String? = null,

    @field:NotBlank(message = "Color cannot be blank")
    var color: String,

    @field:NotNull(message = "Cost cannot be null")
    @field:DecimalMin(value = "0.01", message = "Cost must be greater than 0")
    var cost: BigDecimal,

    @field:NotNull(message = "Edible indicator cannot be null")
    var edible: Boolean
)
