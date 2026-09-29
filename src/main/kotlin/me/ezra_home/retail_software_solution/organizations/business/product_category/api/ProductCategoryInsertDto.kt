package me.ezra_home.retail_software_solution.organizations.business.product_category.api

import java.io.Serializable

data class ProductCategoryInsertDto(
    val categoryName: String? = null,
    val description: String? = null,
) : Serializable
