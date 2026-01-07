package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.ProductRepository
import me.ezra_home.retail_software_solution.organizations.model.ProductTagEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductTagService(
    private val productTagCache: ProductTagCache,
    private val productRepository: ProductRepository,
    private val productTagValidator: ProductTagValidator
) {

    fun manageProductTags(productId: UUID,
                          tagsToAdd: Set<UUID> = emptySet(),
                          tagsToRemove: Set<UUID> = emptySet()
    ) {
        if (!productRepository.existsById(productId)) {
           throw RtsGenericException("Product with ID $productId does not exist")
        }

        val combinedTags = tagsToAdd + tagsToRemove
        productTagValidator.validateTagsExist(combinedTags)
        productTagValidator.validateNoOverlap(tagsToAdd, tagsToRemove)

        val activeProductTags = productTagCache.findActiveProductTagsByProductId(productId)
        val activeTagIds = activeProductTags.map { it.tagId }

        val entitiesToUpdate = mutableListOf<ProductTagEntity>()

        tagsToRemove.forEach { tagId ->
            val existingAssignment = activeProductTags.find { it.tagId == tagId }
            if (existingAssignment != null) {
                existingAssignment.endOn = OffsetDateTime.now()
                entitiesToUpdate.add(existingAssignment)
            }
        }

        tagsToAdd.forEach { tagId ->
            if (!activeTagIds.contains(tagId)) {
                val newProductTag = ProductTagEntity(
                    productId = productId,
                    tagId = tagId
                )
                entitiesToUpdate.add(newProductTag)
            }
        }

        if (entitiesToUpdate.isNotEmpty()) {
            productTagCache.saveAllProductTags(entitiesToUpdate)
        }
    }
}
