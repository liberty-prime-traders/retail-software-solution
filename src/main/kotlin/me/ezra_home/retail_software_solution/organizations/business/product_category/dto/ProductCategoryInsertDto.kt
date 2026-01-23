package me.ezra_home.retail_software_solution.organizations.business.product_category.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductCategoryEntity}
 */
data class ProductCategoryInsertDto(
    val categoryName: String? = null,
    val description: String? = null,
) : Serializable
