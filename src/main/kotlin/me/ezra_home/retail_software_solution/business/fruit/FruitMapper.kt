package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
abstract class FruitMapper {

    abstract fun toEntity(fruitResponseDto: FruitResponseDto): FruitEntity

    abstract fun toDto(fruitEntity: FruitEntity): FruitResponseDto

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    abstract fun partialUpdate(fruitResponseDto: FruitResponseDto, @MappingTarget fruitEntity: FruitEntity): FruitEntity
}