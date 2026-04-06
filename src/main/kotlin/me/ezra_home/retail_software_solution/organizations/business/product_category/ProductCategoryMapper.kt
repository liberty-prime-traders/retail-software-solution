package me.ezra_home.retail_software_solution.organizations.business.product_category

import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryUpdateDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface ProductCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toDomainDto(productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryDto

    fun toDomainDto(productCategoryEntity: ProductCategoryEntity): ProductCategoryDto

    fun toEntity(productCategoryDto: ProductCategoryDto): ProductCategoryEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(productCategoryDto: ProductCategoryDto): ProductCategoryResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(productCategoryUpdateDto: ProductCategoryUpdateDto, @MappingTarget productCategoryDto: ProductCategoryDto)
}
