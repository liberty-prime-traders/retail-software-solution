package me.ezra_home.retail_software_solution.organizations.business.product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.catalog.CatalogEventHandler
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductCache
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductDto
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductValidator
import me.ezra_home.retail_software_solution.organizations.business.product_tag.api.ProductTagService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
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
    private val catalogEventHandler: CatalogEventHandler,
    private val unitValueService: UnitValueService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun countAllProducts(): Long = organizationProductCache.countAllProducts()

    fun createProduct(productInsertDto: OrganizationProductInsertDto): OrganizationProductResponseDto {
        organizationProductValidator.validateProductInsert(productInsertDto)
        val dto = organizationProductMapper.toDomainDto(productInsertDto)
        dto.status = ProductStatus.ACTIVE
        val savedDto = organizationProductCache.upsertProduct(dto)
        if (savedDto.id != null) {
            productTagService.manageProductTags(
                productId = savedDto.id!!,
                tagsToAdd = productInsertDto.tagsToAdd
            )
        }
        catalogEventHandler.publish(TableName.PRODUCT, savedDto.id!!)
        return organizationProductMapper.toResponseDto(savedDto, unitValueService.getUnitName(savedDto.baseUnitId))
    }

    fun updateProduct(productUpdateDto: OrganizationProductUpdateDto): OrganizationProductResponseDto {
        organizationProductRepository.findById(productUpdateDto.id).orElseThrow { UpdatingNonExistingRecordException() }
        organizationProductValidator.validateProductUpdate(productUpdateDto)
        val dto = organizationProductCache.findAllProducts().find { it.id == productUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()
        organizationProductMapper.partialUpdate(productUpdateDto, dto)
        val savedDto = organizationProductCache.upsertProduct(dto)
        productTagService.manageProductTags(
            productId = productUpdateDto.id,
            tagsToAdd = productUpdateDto.tagsToAdd,
            tagsToRemove = productUpdateDto.tagsToRemove
        )
        catalogEventHandler.publish(TableName.PRODUCT, savedDto.id!!)
        return organizationProductMapper.toResponseDto(savedDto, unitValueService.getUnitName(savedDto.baseUnitId))
    }

    fun deactivateProduct(productId: UUID): OrganizationProductResponseDto {
        val dto = organizationProductCache.findAllProducts().find { it.id == productId }
            ?: throw UpdatingNonExistingRecordException()
        return updateStatus(dto, ProductStatus.DISCONTINUED)
    }

    fun reactivateProduct(productId: UUID): OrganizationProductResponseDto {
        val dto = organizationProductCache.findAllProducts().find { it.id == productId }
            ?: throw UpdatingNonExistingRecordException()
        return updateStatus(dto, ProductStatus.ACTIVE)
    }

    private fun updateStatus(productDto: OrganizationProductDto, status: ProductStatus): OrganizationProductResponseDto {
        productDto.status = status
        val savedDto = organizationProductCache.upsertProduct(productDto)
        catalogEventHandler.publish(TableName.PRODUCT, savedDto.id!!)
        return organizationProductMapper.toResponseDto(savedDto, unitValueService.getUnitName(savedDto.baseUnitId))
    }

}
