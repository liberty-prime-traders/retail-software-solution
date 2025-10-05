package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.category.CategoryCache
import me.ezra_home.retail_software_solution.organizations.business.category.CategoryUsageCounter
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.service.OrganizationReferenceNumberGeneratorService
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductService(
    private val productMapper: ProductMapper,
    private val productCache: ProductCache,
    private val categoryCache: CategoryCache,
    private val categoryUsageCounter: CategoryUsageCounter,
    private val productValidator: ProductValidator,
    private val referenceNumberGeneratorService: OrganizationReferenceNumberGeneratorService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllProducts(): Collection<ProductResponseDto> {
        return productCache.getAllProducts().map { productMapper.toDto(it) }
    }

    fun createProduct(productInsertDto: ProductInsertDto): ProductResponseDto {
        productValidator.validateProductInsert(productInsertDto)
        val productEntity = productMapper.toEntity(productInsertDto)
        productEntity.referenceNumber = referenceNumberGeneratorService.generateReferenceNumber(TableNames.PRODUCT)
        categoryUsageCounter.incrementUsageCount(productInsertDto.categoryId)
        productCache.upsertProducts(productEntity)
        return productMapper.toDto(productEntity)
    }

    fun updateProduct(productDto: ProductUpdateDto): ProductResponseDto {
        val productToUpdate = productCache.getAllProducts().find { Objects.equals(productDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        productValidator.validateProductUpdate(productDto)
        if (productDto.categoryId != null) {
            categoryCache.getAllCategories().find { it.id == productToUpdate.categoryId }?.let {
                categoryUsageCounter.decrementUsageCount(it)
            }
            categoryCache.getAllCategories().find { it.id == productDto.categoryId.get() }?.let {
                categoryUsageCounter.incrementUsageCount(it)
            }
        }
        productMapper.partialUpdate(productDto, productToUpdate)
        productCache.upsertProducts(productToUpdate)
        return productMapper.toDto(productToUpdate)
    }

    fun deleteProduct(id: UUID?) {
        id?.let {
            productCache.getAllProducts().find { it.id == id }?.let { entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Product ${entity.productName} has $usageCount usage(s) and cannot be deleted")
                }
                productCache.deleteProduct(id)
            }
        }
    }
}
