package me.ezra_home.retail_software_solution.business.variation

import me.ezra_home.retail_software_solution.business.variation.dto.CreateVariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationEntityDto
import org.mapstruct.*
import java.util.List

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
abstract class VariationEntityMapper {

    abstract fun toEntity(variationEntityDto: CreateVariationDto): VariationEntity

    abstract fun toDto(variationEntity: VariationEntity): VariationEntityDto

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(
        variationEntityDto: VariationEntityDto,
        @MappingTarget variationEntity: VariationEntity
    ): VariationEntity

    fun toDtoList(variations: List<VariationEntity>) {

    }
}
