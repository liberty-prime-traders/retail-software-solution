package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupUpdateDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface ProductGroupMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdById", ignore = true)
  @Mapping(target = "createdOn", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  fun toDomainDto(productGroupInsertDto: ProductGroupInsertDto): ProductGroupDto

  fun toDomainDto(productGroupEntity: ProductGroupEntity): ProductGroupDto

  fun toEntity(productGroupDto: ProductGroupDto): ProductGroupEntity

  @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
  @Mapping(target = "categoryName", expression = "java(categoryName)")
  fun toResponseDto(productGroupDto: ProductGroupDto, @Context categoryName: String?): ProductGroupResponseDto

  @Mapping(target = "createdById", ignore = true)
  @Mapping(target = "createdOn", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  fun partialUpdate(productGroupUpdateDto: ProductGroupUpdateDto, @MappingTarget productGroupDto: ProductGroupDto)
}
