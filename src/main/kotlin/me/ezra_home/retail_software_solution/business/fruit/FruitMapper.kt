package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDto
import me.ezra_home.retail_software_solution.business.util.mapper.RtsMapperConfig
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface FruitMapper {
    fun toDto(fruitEntity: FruitEntity): FruitResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(fruitInsertDto: FruitInsertDto): FruitEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(fruitDto: FruitUpdateDto, @MappingTarget fruitEntity: FruitEntity)
}