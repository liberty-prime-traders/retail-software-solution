package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT])
class OrganizationProductCache(
    private val organizationProductRepository: OrganizationProductRepository,
    private val organizationProductMapper: OrganizationProductMapper
) {

    @Cacheable
    fun findAllProducts(): List<OrganizationProductDto> =
        organizationProductRepository.findAllProducts().map { organizationProductMapper.toDomainDto(it) }

    @Cacheable
    fun countAllProducts(): Long = organizationProductRepository.count()

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrganizationProductInsertDto): OrganizationProductDto {
        val entity = organizationProductMapper.toEntity(insertDto).apply { status = ProductStatus.ACTIVE }
        val saved = organizationProductRepository.save(entity)
        return organizationProductMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(productDto: OrganizationProductDto): OrganizationProductDto {
        val saved = organizationProductRepository.save(organizationProductMapper.toEntity(productDto))
        return organizationProductMapper.toDomainDto(saved)
    }
}
