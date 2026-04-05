package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product_tag.dto.ProductTagDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductTagService(
    private val productTagCache: ProductTagCache,
    private val organizationProductRepository: OrganizationProductRepository,
    private val productTagValidator: ProductTagValidator
) {

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

        val dtosToUpdate = mutableListOf<ProductTagDto>()

        tagsToRemove.forEach { tagId ->
            val existingAssignment = activeProductTags.find { it.tagId == tagId }
            if (existingAssignment != null) {
                existingAssignment.endOn = OffsetDateTime.now()
                dtosToUpdate.add(existingAssignment)
            }
        }

        tagsToAdd.forEach { tagId ->
            if (!activeTagIds.contains(tagId)) {
                dtosToUpdate.add(ProductTagDto(productId = productId, tagId = tagId))
            }
        }

        if (dtosToUpdate.isNotEmpty()) {
            productTagCache.saveAllProductTags(dtosToUpdate)
        }
    }
}
