package me.ezra_home.retail_software_solution.organizations.business.product_tag.api

import java.io.Serializable
import java.util.UUID

data class ProductTagInsertDto(
    val productId: UUID,
    val tagId: UUID
) : Serializable
