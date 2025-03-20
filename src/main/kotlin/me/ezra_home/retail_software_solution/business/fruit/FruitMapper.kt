package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDto
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.mapstruct.*

@Mapper(config = RtsMapperConfig::class)
interface FruitMapper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(fruitEntity: FruitEntity): FruitResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(fruitInsertDto: FruitInsertDto): FruitEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(fruitUpdateDto: FruitUpdateDto, @MappingTarget fruitEntity: FruitEntity)
}