package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.organizations.business.product_tag.dto.ProductTagDto
import me.ezra_home.retail_software_solution.organizations.model.ProductTagEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
internal interface ProductTagMapper {

    fun toDomainDto(productTagEntity: ProductTagEntity): ProductTagDto

    fun toEntity(productTagDto: ProductTagDto): ProductTagEntity
}
