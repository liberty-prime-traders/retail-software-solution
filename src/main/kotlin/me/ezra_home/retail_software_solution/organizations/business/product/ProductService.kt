package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagService
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductService(
    private val productMapper: ProductMapper,
    private val productCache: ProductCache,
    private val productRepository: ProductRepository,
    private val productValidator: ProductValidator,
    private val productTagService: ProductTagService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getTopProducts(): Collection<ProductResponseDto> {
        return productCache.findTopProducts().map { productMapper.toDto(it) }
    }

    fun createProduct(productInsertDto: ProductInsertDto): ProductResponseDto {
        productValidator.validateProductInsert(productInsertDto)
        val productEntity = productMapper.toEntity(productInsertDto)
        productEntity.status = ProductStatus.ACTIVE
        productCache.upsertProduct(productEntity)
        if (productEntity.id != null) {
            productTagService.manageProductTags(
                productId = productEntity.id!!,
                tagsToAdd = productInsertDto.tagsToAdd
            )
        }
        return productMapper.toDto(productEntity)
    }

    fun updateProduct(productDto: ProductUpdateDto): ProductResponseDto {
        val productToUpdate = productRepository.findById(productDto.id).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        productValidator.validateProductUpdate(productDto)
        productMapper.partialUpdate(productDto, productToUpdate)
        productCache.upsertProduct(productToUpdate)
        productTagService.manageProductTags(
            productId = productDto.id,
            tagsToAdd = productDto.tagsToAdd,
            tagsToRemove = productDto.tagsToRemove
        )
        return productMapper.toDto(productToUpdate)
    }

    fun deactivateProduct(productId: UUID): ProductResponseDto {
        val productToDeactivate = productRepository.findById(productId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        return updateStatus(productToDeactivate, ProductStatus.DISCONTINUED)
    }

    fun reactivateProduct(productId: UUID): ProductResponseDto {
        val productToDeactivate = productRepository.findById(productId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        return updateStatus(productToDeactivate, ProductStatus.ACTIVE)
    }

    private fun updateStatus(product: ProductEntity, status: ProductStatus): ProductResponseDto {
        product.status = status
        productCache.upsertProduct(product)
        return productMapper.toDto(product)
    }

}
