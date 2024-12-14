package me.ezra_home.retail_software_solution.business.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import me.ezra_home.retail_software_solution.model.entity.FruitEntityDt

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface FruitMapper {
    fun toDto(entity: FruitEntity): FruitEntityDto

    @Mapping(target = "id", expression = "java(dto.getId() ?: java.util.UUID.randomUUID())")
    fun toEntity(dto: FruitEntityDto): FruitEntity

    fun updateEntity(dto: FruitEntityDto, @MappingTarget entity: FruitEntity): FruitEntity
}