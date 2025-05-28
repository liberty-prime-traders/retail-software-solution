package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.organizations.business.category.CategoryService
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

@Mapper(config = RtsMapperConfig::class)
abstract class ProductMapper {

    @Autowired
    protected lateinit var categoryService: CategoryService

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(target = "categoryName", expression = "java(resolveCategoryName(productEntity.getCategoryId()))")
    abstract fun toDto(productEntity: ProductEntity): ProductResponseDto

    protected fun resolveCategoryName(categoryId: UUID?): String? {
        val cachedCategories = categoryService.getCachedCategoryMap()
        return categoryId?.let { cachedCategories[it]?.categoryName }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    abstract fun toEntity(productInsertDto: ProductInsertDto): ProductEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(productDto: ProductUpdateDto, @MappingTarget productEntity: ProductEntity)
}
