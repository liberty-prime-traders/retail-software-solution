package me.ezra_home.retail_software_solution.business.mapper

import org.springframework.stereotype.Component
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import me.ezra_home.retail_software_solution.model.entity.FruitEntityDto
import java.util.UUID

@Component
class FruitMapper {

    // Convert FruitEntity to FruitEntityDto
    fun toDto(entity: FruitEntity): FruitEntityDto {
        return FruitEntityDto(
            id = entity.id, // Transfer ID as-is
            name = entity.name, // Map the name field
            alternateName = entity.alternateName, // Optional alternate name
            color = entity.color, // Map the color field
            cost = entity.cost, // Map the cost field
            edibleInd = entity.edibleInd, // Map whether it's edible
            createdById = entity.createdById, // Map the creator's ID
            createdOn = entity.createdOn // Map the created timestamp
        )
    }

    // Convert FruitEntityDto to FruitEntity
    fun toEntity(dto: FruitEntityDto): FruitEntity {
        return FruitEntity().apply {
            id = dto.id ?: UUID.randomUUID() // Generate a UUID if ID is null
            name = dto.name // Map the name field
            alternateName = dto.alternateName // Optional alternate name
            color = dto.color // Map the color field
            cost = dto.cost // Map the cost field
            edibleInd = dto.edibleInd // Map whether it's edible
            createdById = dto.createdById // Map the creator's ID
            createdOn = dto.createdOn // Map the created timestamp
        }
    }
}