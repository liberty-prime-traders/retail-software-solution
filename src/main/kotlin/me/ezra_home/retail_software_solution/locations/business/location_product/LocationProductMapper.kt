package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductInsertDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface LocationProductMapper {

    fun toDomainDto(entity: LocationProductEntity): LocationProductDto

    fun toEntity(dto: LocationProductDto): LocationProductEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "defaultSalePrice", ignore = true)
    @Mapping(target = "minStockLevel", ignore = true)
    @Mapping(target = "lastPurchasePrice", ignore = true)
    @Mapping(target = "stockBalance", ignore = true)
    fun toEntity(insertDto: LocationProductInsertDto): LocationProductEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(target = "baseUnit", expression = "java(baseUnitName)")
    fun toDto(dto: LocationProductDto, @Context baseUnitName: String?): LocationProductResponseDto
}
