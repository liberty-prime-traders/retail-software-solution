package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface LocationProductMapper {

  @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
  fun toDto(entity: LocationProductEntity): LocationProductResponseDto

  @Mapping(target = "productId", ignore = true)
  @Mapping(target = "productName", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "productGroupName", ignore = true)
  @Mapping(target = "categoryId", ignore = true)
  @Mapping(target = "baseUnitId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  @Mapping(target = "createdById", ignore = true)
  @Mapping(target = "createdOn", ignore = true)
  @Mapping(target = "lastSyncedAt", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  fun partialUpdate(dto: LocationProductUpdateDto, @MappingTarget entity: LocationProductEntity)
}
