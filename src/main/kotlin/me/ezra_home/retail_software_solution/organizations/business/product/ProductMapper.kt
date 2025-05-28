package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.category.CategoryName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy
import java.util.UUID

@Mapper(config = RtsMapperConfig::class)
interface ProductMapper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "categoryId", target = "categoryName", qualifiedBy = [CategoryName::class])
    abstract fun toDto(productEntity: ProductEntity): ProductResponseDto

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
