package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.organizations.business.product_tag.api.ProductTagInsertDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ProductTagMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: ProductTagInsertDto): ProductTagEntity

    fun toDomainDto(productTagEntity: ProductTagEntity): ProductTagDto

    fun toEntity(productTagDto: ProductTagDto): ProductTagEntity
}
