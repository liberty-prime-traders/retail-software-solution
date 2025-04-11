package me.ezra_home.retail_software_solution.locations.business.variation

import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationInsertDto
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationResponseDto
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationUpdateDto
import me.ezra_home.retail_software_solution.locations.model.VariationEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface VariationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(variationInsertDto: VariationInsertDto): VariationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(variationEntity: VariationEntity): VariationResponseDto


    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(variationUpdateDto: VariationUpdateDto, @MappingTarget variationEntity: VariationEntity)
}
