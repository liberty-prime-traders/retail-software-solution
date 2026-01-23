package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.product.mapping.ProductCategoryId
import me.ezra_home.retail_software_solution.organizations.business.product.mapping.ProductCategoryName
import me.ezra_home.retail_software_solution.organizations.business.product.mapping.ProductCategoryQualifier
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ActiveProductTags
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitName
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueQualifier
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [ProductCategoryQualifier::class, UnitValueQualifier::class, ProductTagQualifier::class]
)
interface ProductMapper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "productGroupId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
    @Mapping(source = "productGroupId", target = "categoryId", qualifiedBy = [ProductCategoryId::class])
    @Mapping(source = "baseUnitId", target = "baseUnit", qualifiedBy = [UnitName::class])
    @Mapping(source = "id", target = "activeTags", qualifiedBy = [ActiveProductTags::class])
    fun toDto(productEntity: ProductEntity): ProductResponseDto

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "productGroupId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
    @Mapping(source = "productGroupId", target = "categoryId", qualifiedBy = [ProductCategoryId::class])
    @Mapping(source = "baseUnitId", target = "baseUnit", qualifiedBy = [UnitName::class])
    @Mapping(target = "activeTags", ignore = true)
    fun toDtoWithoutTags(productEntity: ProductEntity): ProductResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productGroupName", ignore = true)
    fun toEntity(productInsertDto: ProductInsertDto): ProductEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productGroupName", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(productDto: ProductUpdateDto, @MappingTarget productEntity: ProductEntity)
}
