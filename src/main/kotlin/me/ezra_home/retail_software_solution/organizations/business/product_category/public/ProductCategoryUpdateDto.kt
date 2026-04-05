package me.ezra_home.retail_software_solution.organizations.business.product_category.public

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class ProductCategoryUpdateDto(
    val id: UUID? = null,
    val categoryName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable
