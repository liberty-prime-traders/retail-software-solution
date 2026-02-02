package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ActiveProductTags
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitName
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueQualifier
import me.ezra_home.retail_software_solution.organizations.model.OrganizationProductEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [OrganizationProductCategoryQualifier::class, UnitValueQualifier::class, ProductTagQualifier::class]
)
interface OrganizationProductMapper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "productGroupId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
    @Mapping(source = "productGroupId", target = "categoryId", qualifiedBy = [ProductCategoryId::class])
    @Mapping(source = "baseUnitId", target = "baseUnit", qualifiedBy = [UnitName::class])
    @Mapping(source = "id", target = "activeTags", qualifiedBy = [ActiveProductTags::class])
    fun toDto(productEntity: OrganizationProductEntity): OrganizationProductResponseDto

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "productGroupId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
    @Mapping(source = "productGroupId", target = "categoryId", qualifiedBy = [ProductCategoryId::class])
    @Mapping(source = "baseUnitId", target = "baseUnit", qualifiedBy = [UnitName::class])
    @Mapping(target = "activeTags", ignore = true)
    fun toDtoWithoutTags(productEntity: OrganizationProductEntity): OrganizationProductResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productGroupName", ignore = true)
    fun toEntity(productInsertDto: OrganizationProductInsertDto): OrganizationProductEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productGroupName", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(productDto: OrganizationProductUpdateDto, @MappingTarget productEntity: OrganizationProductEntity)
}
