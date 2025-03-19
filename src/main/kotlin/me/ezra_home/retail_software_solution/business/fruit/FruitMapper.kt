package me.ezra_home.retail_software_solution.model.mapper

import me.ezra_home.retail_software_solution.model.dto.FruitInsertDto
import me.ezra_home.retail_software_solution.model.dto.FruitUpdateDto
import me.ezra_home.retail_software_solution.model.dto.FruitResponseDto
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

@Mapper(componentModel = "spring")
interface FruitMapper {

    companion object {
        val INSTANCE: FruitMapper = Mappers.getMapper(FruitMapper::class.java)
    }

    // Converts Entity to Response DTO
    @Mapping(source = "cost", target = "cost", numberFormat = "Ksh #,###.00")
    fun toResponseDTO(entity: FruitEntity): FruitResponseDTO

    // Converts Request DTO to Entity
    fun toEntity(request: FruitRequestDTO): FruitEntity
}
