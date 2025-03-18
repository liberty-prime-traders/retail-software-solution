package me.ezra_home.retail_software_solution.model.mapper

import me.ezra_home.retail_software_solution.model.dto.FruitInsertDto
import me.ezra_home.retail_software_solution.model.dto.FruitUpdateDto
import me.ezra_home.retail_software_solution.model.dto.FruitResponseDto
import me.ezra_home.retail_software_solution.model.entity.FruitEntity

fun FruitRequestDTO.toEntity(): FruitEntity {
    return FruitEntity(
        name = this.name,
        alternateName = this.alternateName,
        color = this.color,
        cost = this.cost,
        edible = this.edible
    )
}

fun FruitEntity.toResponseDTO(): FruitResponseDTO {
    return FruitResponseDTO(
        id = this.id!!,
        name = this.name,
        alternateName = this.alternateName,
        color = this.color,
        cost = this.cost,
        edible = this.edible,
        createdById = this.createdById,
        createdOn = this.createdOn,
        predecessorOfId = this.predecessorOfId,
        usageCount = this.usageCount
    )
}
