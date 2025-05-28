package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.category.CategoryCache
import me.ezra_home.retail_software_solution.organizations.business.category.CategoryUsageCounter
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductService(
    private val productMapper: ProductMapper,
    private val productCache: ProductCache,
    private val categoryCache: CategoryCache,
    private val categoryUsageCounter: CategoryUsageCounter
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllProducts(): Collection<ProductResponseDto> {
        return productCache.getAllProducts().map { productMapper.toDto(it) }
    }

    fun createProduct(productInsertDto: ProductInsertDto): ProductResponseDto {
        validateProductInsert(productInsertDto)
        val productEntity = productMapper.toEntity(productInsertDto)
        categoryUsageCounter.incrementUsageCount(productInsertDto.categoryId)
        productCache.upsertProducts(productEntity)
        return productMapper.toDto(productEntity)
    }

    fun updateProduct(productDto: ProductUpdateDto): ProductResponseDto {
        val productToUpdate = productCache.getAllProducts().find { Objects.equals(productDto.id, it.id) }
        if (productToUpdate == null) throw UpdatingNonExistingRecordException()
        validateProductUpdate(productDto, productToUpdate)
        if (productDto.categoryId != null) {
            categoryCache.getAllCategories().find { it.id == productToUpdate.categoryId }?.let {
                categoryUsageCounter.decrementUsageCount(it)
            }
            categoryCache.getAllCategories().find { it.id == productDto.categoryId.get() }?.let {
                categoryUsageCounter.incrementUsageCount(it)
            }
        }
        productMapper.partialUpdate(productDto, productToUpdate)
        val updatedProduct = productCache.upsertProducts(productToUpdate)
        return productMapper.toDto(updatedProduct)
    }

    private fun validateProductUpdate(productUpdateDto: ProductUpdateDto, productToUpdate: ProductEntity) {
        val name = StringUtils.getValueOrException(productUpdateDto.productName, NAME_IS_REQUIRED)
        productCache.getAllProducts()
            .find { StringUtils.isEquivalent(it.productName, name) && it.id != productUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
        if (productUpdateDto.categoryId != null && categoryCache.getAllCategories().none { it.id == productUpdateDto.categoryId.get()}){
            throw RtsGenericException(INVALID_CATEGORY_ID)
        }
        else if(productUpdateDto.categoryId != null && productUpdateDto.categoryId.get() == productToUpdate.categoryId) {
            throw RtsGenericException(CATEGORY_TYPE_DO_NOT_MATCH)
        }
    }

    private fun validateProductInsert(productInsertDto: ProductInsertDto) {
        val name = StringUtils.getValueOrException(productInsertDto.productName, NAME_IS_REQUIRED)
        productCache.getAllProducts()
            .find { StringUtils.isEquivalent(it.productName, name)}
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
        if (productInsertDto.categoryId == null){
            throw RtsGenericException(CATEGORY_ID_REQUIRED)
        }
        else if(categoryCache.getAllCategories().none { it.id == productInsertDto.categoryId}){
            throw RtsGenericException(INVALID_CATEGORY_ID)
        }
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
    
    companion object {
        const val NAME_IS_REQUIRED = "A product must have a name"
        const val NAME_ALREADY_EXISTS = "A product with the name %s already exists."
        const val INVALID_CATEGORY_ID = "The category ID provided does not exists."
        const val CATEGORY_ID_REQUIRED = "The category ID is required."
        const val CATEGORY_TYPE_DO_NOT_MATCH = "The category type provided do not match."
    }
    
}
