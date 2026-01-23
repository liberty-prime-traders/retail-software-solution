package me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping

import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.TagSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagCache
import me.ezra_home.retail_software_solution.organizations.business.tag.TagCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductTagQualifier(
    private val productTagCache: ProductTagCache,
    private val tagCache: TagCache
) {

    @ActiveProductTags
    fun mapProductIdToActiveTags(productId: UUID?): List<TagSummaryDto> {
        if (productId == null) return emptyList()

        val activeProductTagIds = productTagCache.findActiveTagIdsByProductId(productId)
        if (activeProductTagIds.isEmpty()) return emptyList()

        return tagCache.getAllTags()
            .filter { activeProductTagIds.contains(it.id) }
            .map { tag -> TagSummaryDto(id = tag.id, tagName = tag.tagName) }
    }

    fun populateTagsForProducts(products: List<ProductResponseDto>): List<ProductResponseDto> {
        if (products.isEmpty()) return products

        val productIds = products.mapNotNull { it.id }
        if (productIds.isEmpty()) return products

        val productTags = productTagCache.findActiveProductTagsByProductIds(productIds)
        val tagIdsByProductId = productTags.groupBy({it.productId},  { it.tagId })

        val tagsById = tagCache.getAllTags().associateBy { it.id }

        return products.map { product ->
            val tagIds = tagIdsByProductId[product.id] ?: emptyList()
            val tags = tagIds.mapNotNull { tagId ->
                tagsById[tagId]?.let { tag -> TagSummaryDto(id = tag.id, tagName = tag.tagName) }
            }
            product.copy(activeTags = tags)
        }
    }
}
