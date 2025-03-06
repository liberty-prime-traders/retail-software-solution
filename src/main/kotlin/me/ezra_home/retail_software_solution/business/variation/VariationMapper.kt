package me.ezra_home.retail_software_solution.business.variation

import me.ezra_home.retail_software_solution.business.variation.dto.VariationCreateDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationUpdateRequest
import me.ezra_home.retail_software_solution.model.entity.VariationEntity
import org.mapstruct.*

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
abstract class VariationMapper {

    abstract fun toEntity(createDto: VariationCreateDto): VariationEntity

    abstract fun toEntity(variationDto: VariationDto): VariationEntity

    abstract fun toDto(variationEntity: VariationEntity): VariationDto

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(
        variationDto: VariationUpdateRequest,
        @MappingTarget variationEntity: VariationEntity
    ): VariationEntity
}