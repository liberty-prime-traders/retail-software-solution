package me.ezra_home.retail_software_solution.organizations.business.product_tag.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.TagSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagCache
import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagValidator
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductTagService(
    private val productTagCache: ProductTagCache,
    private val organizationProductRepository: OrganizationProductRepository,
    private val productTagValidator: ProductTagValidator,
    private val tagService: TagService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun findActiveTagIdsByProductId(productId: UUID): Collection<UUID> =
        productTagCache.findActiveTagIdsByProductId(productId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun populateTagsForProducts(products: List<OrganizationProductResponseDto>): List<OrganizationProductResponseDto> {
        if (products.isEmpty()) return products
        val productIds = products.mapNotNull { it.id }
        val productTags = productTagCache.findActiveProductTagsByProductIds(productIds)
        val tagIdsByProductId = productTags.groupBy({ it.productId }, { it.tagId })
        val tagsById = tagService.getAllTagDtos().associateBy { it.id }
        return products.map { product ->
            val tagIds = tagIdsByProductId[product.id] ?: emptyList()
            val tags = tagIds.mapNotNull { tagId ->
                tagsById[tagId]?.let { tag -> TagSummaryDto(id = tag.id, tagName = tag.tagName) }
            }
            product.copy(activeTags = tags)
        }
    }

    fun manageProductTags(productId: UUID,
                          tagsToAdd: Set<UUID> = emptySet(),
                          tagsToRemove: Set<UUID> = emptySet()
    ) {
        if (!organizationProductRepository.existsById(productId)) {
           throw RtsGenericException("Product with ID $productId does not exist")
        }

        val combinedTags = tagsToAdd + tagsToRemove
        productTagValidator.validateTagsExist(combinedTags)
        productTagValidator.validateNoOverlap(tagsToAdd, tagsToRemove)

        val activeProductTags = productTagCache.findActiveProductTagsByProductId(productId)
        val activeTagIds = activeProductTags.map { it.tagId }

        val dtosToUpdate = tagsToRemove.mapNotNull { tagId ->
            activeProductTags.find { it.tagId == tagId }?.copy(endOn = OffsetDateTime.now())
        }

        if (dtosToUpdate.isNotEmpty()) {
            productTagCache.saveAllProductTags(dtosToUpdate)
        }

        val insertDtos = tagsToAdd
            .filter { !activeTagIds.contains(it) }
            .map { tagId -> ProductTagInsertDto(productId = productId, tagId = tagId) }

        if (insertDtos.isNotEmpty()) {
            productTagCache.createProductTags(insertDtos)
        }
    }
}
