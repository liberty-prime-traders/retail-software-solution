package me.ezra_home.retail_software_solution.business.fruit

import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDTO
import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FruitService(
    private val fruitMapper: FruitMapper,
    private val fruitCache: FruitCache
) {

    @Transactional
    fun getAllFruits(): Collection<FruitResponseDTO> {
        return fruitCache.getAllFruits().map { fruitMapper.toResponseDto(it) }
    }

    @Transactional
    fun createFruit(fruitInsertDTO: FruitInsertDTO): FruitResponseDTO {
        // Validation (if necessary)
        validateNameOnSave(fruitInsertDTO.name)

        // Converting DTO to entity and saving it to the cache
        val entity = fruitMapper.toEntity(fruitInsertDTO)
        fruitCache.upsertFruit(entity)

        // Returning the created fruit as a response DTO
        return fruitMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(name: String?) {
        if (name.isNullOrEmpty()) {
            throw RtsGenericException("A fruit must have a name")
        }

        val fruitWithMatchingName = fruitCache.getAllFruits().find {
            it.name == name
        }

        if (fruitWithMatchingName != null) {
            throw RtsGenericException("A fruit with the name '$name' already exists")
        }
    }

    @Transactional
    fun updateFruit(fruitUpdateDTO: FruitUpdateDTO): FruitResponseDTO {
        val id = fruitUpdateDTO.id ?: throw QueriedByEmptyIdException()

        // Fetching the fruit from the cache and validating
        val entityFromCache = fruitCache.getAllFruits().find { it.id == id } ?: throw ChangeSetPersister.NotFoundException()
        validateNameOnSave(fruitUpdateDTO.name)

        // Mapping the updates
        fruitMapper.partialUpdate(fruitUpdateDTO, entityFromCache)

        // Saving updated entity to the cache
        fruitCache.upsertFruit(entityFromCache)

        // Returning updated fruit as a response DTO
        return fruitMapper.toResponseDto(entityFromCache)
    }

    @Transactional
    fun deleteFruit(id: UUID?) {
        if (id != null) {
            // Fetching the fruit entity from cache
            val entity = fruitCache.getAllFruits().find { it.id == id }
            if (entity != null) {
                val usageCount = entity.usageCount
                if (usageCount != null && usageCount > 0L) {
                    throw RtsGenericException("Fruit ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                fruitCache.deleteFruit(id) // Deleting fruit from the cache
            }
        }
    }
}

