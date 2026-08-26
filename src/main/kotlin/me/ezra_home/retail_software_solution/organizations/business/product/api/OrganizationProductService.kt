package me.ezra_home.retail_software_solution.organizations.business.product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.catalog.CatalogEventHandler
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductCache
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductDto
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductValidator
import me.ezra_home.retail_software_solution.organizations.business.product_tag.api.ProductTagService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
    private val unitValueFetcher: UnitValueFetcher
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun verifyProductIsActive(orgProductId: UUID) {
        val dto = organizationProductCache.findAllProducts().find { it.id == orgProductId }
            ?: throw UpdatingNonExistingRecordException()
        if (dto.status != ProductStatus.ACTIVE) {
            throw RtsGenericException("Cannot reactivate location product: organization product is not active")
        }
    }

    fun createProduct(productInsertDto: OrganizationProductInsertDto): OrganizationProductResponseDto {
        organizationProductValidator.validateProductInsert(productInsertDto)
        val savedDto = organizationProductCache.create(productInsertDto)
        productTagService.manageProductTags(
            orgProductId = savedDto.id,
            tagsToAdd = productInsertDto.tagsToAdd
        )
        catalogEventHandler.publish(TableName.PRODUCT, savedDto.id)
        return organizationProductMapper.toResponseDto(savedDto, unitValueFetcher.getUnitName(savedDto.baseUnitId))
    }

    fun updateProduct(productUpdateDto: OrganizationProductUpdateDto): OrganizationProductResponseDto {
        organizationProductRepository.findById(productUpdateDto.id).orElseThrow { UpdatingNonExistingRecordException() }
        organizationProductValidator.validateProductUpdate(productUpdateDto)
        val existing = organizationProductCache.findAllProducts().find { it.id == productUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()
        val updated = productUpdateDto.applyTo(existing)
        val savedDto = organizationProductCache.save(updated)
        productTagService.manageProductTags(
            orgProductId = productUpdateDto.id,
            tagsToAdd = productUpdateDto.tagsToAdd,
            tagsToRemove = productUpdateDto.tagsToRemove
        )
        catalogEventHandler.publish(TableName.PRODUCT, savedDto.id)
        return organizationProductMapper.toResponseDto(savedDto, unitValueFetcher.getUnitName(savedDto.baseUnitId))
    }

    fun deactivateProduct(orgProductId: UUID): OrganizationProductResponseDto {
        val dto = organizationProductCache.findAllProducts().find { it.id == orgProductId }
            ?: throw UpdatingNonExistingRecordException()
        return updateStatus(dto, ProductStatus.DISCONTINUED)
    }

    fun reactivateProduct(orgProductId: UUID): OrganizationProductResponseDto {
        val dto = organizationProductCache.findAllProducts().find { it.id == orgProductId }
            ?: throw UpdatingNonExistingRecordException()
        return updateStatus(dto, ProductStatus.ACTIVE)
    }

    private fun updateStatus(productDto: OrganizationProductDto, status: ProductStatus): OrganizationProductResponseDto {
        val updated = productDto.copy(status = status)
        val savedDto = organizationProductCache.save(updated)
        catalogEventHandler.publish(TableName.PRODUCT, savedDto.id)
        return organizationProductMapper.toResponseDto(savedDto, unitValueFetcher.getUnitName(savedDto.baseUnitId))
    }
}
