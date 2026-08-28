package me.ezra_home.retail_software_solution.organizations.business.product_category

import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ProductCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryEntity

    fun toDomainDto(productCategoryEntity: ProductCategoryEntity): ProductCategoryDto

    fun toEntity(productCategoryDto: ProductCategoryDto): ProductCategoryEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(productCategoryDto: ProductCategoryDto): ProductCategoryResponseDto
}
