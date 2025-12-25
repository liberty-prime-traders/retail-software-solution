package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.CreatedBy
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.organizations.business.category.mapping.CategoryName
import me.ezra_home.retail_software_solution.organizations.business.category.mapping.CategoryNameQualifier
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitName
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueQualifier
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [CategoryNameQualifier::class, UnitValueQualifier::class]
)
interface ProductMapper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "categoryId", target = "categoryName", qualifiedBy = [CategoryName::class])
    @Mapping(source = "baseUnitId", target = "baseUnit", qualifiedBy = [UnitName::class])
    fun toDto(productEntity: ProductEntity): ProductResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "cursor", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(productInsertDto: ProductInsertDto): ProductEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "cursor", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(productDto: ProductUpdateDto, @MappingTarget productEntity: ProductEntity)
}
