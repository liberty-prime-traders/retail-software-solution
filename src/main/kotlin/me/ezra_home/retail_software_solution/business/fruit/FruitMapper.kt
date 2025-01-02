package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDTO
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface FruitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(fruitInsertDTO: FruitInsertDTO): FruitEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(fruitEntity: FruitEntity): FruitResponseDTO


    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(fruitUpdateDTO: FruitUpdateDTO, @MappingTarget fruitEntity: FruitEntity)
}
