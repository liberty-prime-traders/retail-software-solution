package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
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
    fun toEntity(insertDto: LocationProductInsertDto): LocationProductEntity

    @Mapping(target = "baseUnit", expression = "java(ctx.getUnitName())")
    @Mapping(target = "stockBalance", expression = "java(ctx.getBalance())")
    fun toResponseDto(dto: LocationProductDto, @Context ctx: LocationProductContext): LocationProductResponseDto
}
