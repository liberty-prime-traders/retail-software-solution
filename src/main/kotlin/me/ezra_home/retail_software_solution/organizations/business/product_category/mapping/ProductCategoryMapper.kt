package me.ezra_home.retail_software_solution.organizations.business.product_category.mapping

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.ProductCategoryEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
internal interface ProductCategoryMapper {
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(productCategoryEntity: ProductCategoryEntity): ProductCategoryResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(productCategoryDto: ProductCategoryUpdateDto, @MappingTarget productCategoryEntity: ProductCategoryEntity)
}
