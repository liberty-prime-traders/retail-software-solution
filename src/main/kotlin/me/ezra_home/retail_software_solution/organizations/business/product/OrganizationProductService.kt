package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.catalog.CatalogEventHandler
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagService
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.model.OrganizationProductEntity
import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationProductService(
    private val organizationProductMapper: OrganizationProductMapper,
    private val organizationProductCache: OrganizationProductCache,
    private val organizationProductRepository: OrganizationProductRepository,
    private val organizationProductValidator: OrganizationProductValidator,
    private val productTagService: ProductTagService,
    private val productTagQualifier: ProductTagQualifier,
    private val catalogEventHandler: CatalogEventHandler
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun findAllProducts(): List<OrganizationProductResponseDto> {
        val dtos = organizationProductCache.findAllProducts().map { organizationProductMapper.toDtoWithoutTags(it) }
        return productTagQualifier.populateTagsForProducts(dtos)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun countAllProducts(): Long = organizationProductCache.countAllProducts()

    fun createProduct(productInsertDto: OrganizationProductInsertDto): OrganizationProductResponseDto {
        organizationProductValidator.validateProductInsert(productInsertDto)
        val productEntity = organizationProductMapper.toEntity(productInsertDto)
        productEntity.status = ProductStatus.ACTIVE
        organizationProductCache.upsertProduct(productEntity)
        if (productEntity.id != null) {
            productTagService.manageProductTags(
                productId = productEntity.id!!,
                tagsToAdd = productInsertDto.tagsToAdd
            )
        }
        catalogEventHandler.publish(TableName.PRODUCT, productEntity.id!!)
        return organizationProductMapper.toDto(productEntity)
    }

    fun updateProduct(productDto: OrganizationProductUpdateDto): OrganizationProductResponseDto {
        val productToUpdate = organizationProductRepository.findById(productDto.id).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        organizationProductValidator.validateProductUpdate(productDto)
        organizationProductMapper.partialUpdate(productDto, productToUpdate)
        organizationProductCache.upsertProduct(productToUpdate)
        productTagService.manageProductTags(
            productId = productDto.id,
            tagsToAdd = productDto.tagsToAdd,
            tagsToRemove = productDto.tagsToRemove
        )
        catalogEventHandler.publish(TableName.PRODUCT, productToUpdate.id!!)
        return organizationProductMapper.toDto(productToUpdate)
    }

    fun deactivateProduct(productId: UUID): OrganizationProductResponseDto {
        val productToDeactivate = organizationProductRepository.findById(productId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        return updateStatus(productToDeactivate, ProductStatus.DISCONTINUED)
    }

    fun reactivateProduct(productId: UUID): OrganizationProductResponseDto {
        val productToDeactivate = organizationProductRepository.findById(productId).orElseThrow {
            UpdatingNonExistingRecordException()
        }
        return updateStatus(productToDeactivate, ProductStatus.ACTIVE)
    }

    private fun updateStatus(product: OrganizationProductEntity, status: ProductStatus): OrganizationProductResponseDto {
        product.status = status
        organizationProductCache.upsertProduct(product)
        catalogEventHandler.publish(TableName.PRODUCT, product.id!!)
        return organizationProductMapper.toDto(product)
    }

}
