package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
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
    private val productCache: ProductCache
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllCategories(): Collection<ProductResponseDto> {
        return productCache.getAllCategories().map { productMapper.toDto(it) }
    }

    fun createProduct(productInsertDto: ProductInsertDto): ProductResponseDto {
        val productName = StringUtils.getValueOrException(productInsertDto.productName, NAME_IS_REQUIRED)
        productCache.getAllCategories().find { StringUtils.isEquivalent(it.productName, productName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, productName))}
        val newProductEntity = productMapper.toEntity(productInsertDto)
        val savedProductEntity = productCache.upsertCategories(newProductEntity)
        return productMapper.toDto(savedProductEntity)
    }

    fun updateProduct(productDto: ProductUpdateDto): ProductResponseDto {
        validateProductUpdate(productDto)
        val productToUpdate = productCache.getAllCategories().find { Objects.equals(productDto.id, it.id) }
        if (productToUpdate == null) throw UpdatingNonExistingRecordException()
        productMapper.partialUpdate(productDto, productToUpdate)
        val updatedProduct = productCache.upsertCategories(productToUpdate)
        return productMapper.toDto(updatedProduct)
    }

    private fun validateProductUpdate(productUpdateDto: ProductUpdateDto) {
        val name = StringUtils.getValueOrException(productUpdateDto.productName, NAME_IS_REQUIRED)
        productCache.getAllCategories()
            .find { StringUtils.isEquivalent(it.productName, name) && it.id != productUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun deleteProduct(id: UUID?) {
        id?.let {
            productCache.getAllCategories().find { it.id == id }?.let { entity ->
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
        const val NAME_ALREADY_EXISTS = "A product with the name %s is already assigned."
    }
    
}
