package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ActiveProductTags
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [OrganizationProductCategoryQualifier::class, ProductTagQualifier::class]
)
interface OrganizationProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productGroupName", ignore = true)
    fun toDomainDto(productInsertDto: OrganizationProductInsertDto): OrganizationProductDto

    fun toDomainDto(productEntity: OrganizationProductEntity): OrganizationProductDto

    fun toEntity(productDto: OrganizationProductDto): OrganizationProductEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "productGroupId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
    @Mapping(source = "productGroupId", target = "categoryId", qualifiedBy = [ProductCategoryId::class])
    @Mapping(target = "baseUnit", expression = "java(baseUnit)")
    @Mapping(source = "id", target = "activeTags", qualifiedBy = [ActiveProductTags::class])
    fun toResponseDto(productDto: OrganizationProductDto, @Context baseUnit: String?): OrganizationProductResponseDto

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "productGroupId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
    @Mapping(source = "productGroupId", target = "categoryId", qualifiedBy = [ProductCategoryId::class])
    @Mapping(target = "baseUnit", expression = "java(baseUnit)")
    @Mapping(target = "activeTags", ignore = true)
    fun toResponseDtoWithoutTags(productDto: OrganizationProductDto, @Context baseUnit: String?): OrganizationProductResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productGroupName", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(productUpdateDto: OrganizationProductUpdateDto, @MappingTarget productDto: OrganizationProductDto)
}
