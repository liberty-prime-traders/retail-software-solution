package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.ProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.dto.TagSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.dto.ProductTagRequestDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.dto.ProductTagResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tag.TagCache
import me.ezra_home.retail_software_solution.organizations.model.ProductTagEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductTagService(
    private val productTagCache: ProductTagCache,
    private val tagCache: TagCache,
    private val productRepository: ProductRepository,
    private val productTagValidator: ProductTagValidator
) {

    fun manageProductTags(productId: UUID, requestDto: ProductTagRequestDto): ProductTagResponseDto {
        productRepository.findById(productId).orElseThrow {
            RtsGenericException("Product with ID $productId does not exist")
        }

        val combinedTags = requestDto.tagsToAdd + requestDto.tagsToRemove
        productTagValidator.validateTagsExist(combinedTags)

        val activeProductTags = productTagCache.findActiveProductTagsByProductId(productId)
        val activeTagIds = activeProductTags.map { it.tagId }

        val entitiesToUpdate = mutableListOf<ProductTagEntity>()

        requestDto.tagsToRemove.forEach { tagId ->
            val existingAssignment = activeProductTags.find { it.tagId == tagId }
            if (existingAssignment != null) {
                existingAssignment.endOn = OffsetDateTime.now()
                entitiesToUpdate.add(existingAssignment)
            }
        }

        requestDto.tagsToAdd.forEach { tagId ->
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

        val updatedActiveTagIds = productTagCache.findActiveTagIdsByProductId(productId).toSet()
        val updatedActiveTags = tagCache.getAllTags()
            .filter { updatedActiveTagIds.contains(it.id) }
            .map { TagSummaryDto(id = it.id, tagName = it.tagName) }
        return ProductTagResponseDto(
            productId = productId,
            activeTags = updatedActiveTags
        )
    }
}
