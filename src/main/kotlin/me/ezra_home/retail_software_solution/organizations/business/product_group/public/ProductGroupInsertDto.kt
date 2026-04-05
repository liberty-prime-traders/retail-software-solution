package me.ezra_home.retail_software_solution.organizations.business.product_group.public

import java.io.Serializable
import java.util.UUID

data class ProductGroupInsertDto(
    val groupName: String,
    val description: String? = null,
    val categoryId: UUID
) : Serializable
