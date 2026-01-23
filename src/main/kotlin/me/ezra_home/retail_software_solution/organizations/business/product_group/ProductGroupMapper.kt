package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.organizations.business.product_category.mapping.ProductCategoryName
import me.ezra_home.retail_software_solution.organizations.business.product_category.mapping.ProductCategoryNameQualifier
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class, uses = [ProductCategoryNameQualifier::class])
interface ProductGroupMapper {

  @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
  @Mapping(source = "categoryId", target = "categoryName", qualifiedBy = [ProductCategoryName::class])
  fun toDto(productGroupEntity: ProductGroupEntity): ProductGroupResponseDto

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdById", ignore = true)
  @Mapping(target = "createdOn", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  fun toEntity(productGroupInsertDto: ProductGroupInsertDto): ProductGroupEntity

  @Mapping(target = "createdById", ignore = true)
  @Mapping(target = "createdOn", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  fun partialUpdate(productGroupDto: ProductGroupUpdateDto, @MappingTarget productGroupEntity: ProductGroupEntity)
}
