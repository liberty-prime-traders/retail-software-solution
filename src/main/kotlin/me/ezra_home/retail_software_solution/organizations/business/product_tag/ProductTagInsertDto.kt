package me.ezra_home.retail_software_solution.organizations.business.product_tag

import java.io.Serializable
import java.util.UUID

data class ProductTagInsertDto(
    val orgProductId: UUID,
    val tagId: UUID
) : Serializable
