package me.ezra_home.retail_software_solution.organizations.business.product_tag.api

import me.ezra_home.retail_software_solution.organizations.business.product.api.TagSummaryDto
import java.io.Serializable
import java.util.UUID

data class ProductTagResponseDto(
    val productId: UUID,
    val activeTags: Collection<TagSummaryDto>
) : Serializable
