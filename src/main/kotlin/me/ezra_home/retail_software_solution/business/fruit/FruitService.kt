package me.ezra_home.retail_software_solution.business.fruit

import com.google.common.base.Strings
import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDTO
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDTO
import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service
import java.util.*

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
        validateNameOnSave(Optional.ofNullable(fruitInsertDTO.name))

        val entity = fruitMapper.toEntity(fruitInsertDTO)
        fruitCache.upsertFruit(entity)

        return fruitMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(name: Optional<String>?, id: UUID? = null) {
        if (name == null || name.isEmpty || Strings.isNullOrEmpty(name.get()) ) {
            throw RtsGenericException("A fruit must have a name")
        }

        val fruitWithMatchingName = fruitCache.getAllFruits().find {
            it.name == name.get() && !Objects.equals(it.id, id)
        }

        if (fruitWithMatchingName != null) {
            throw RtsGenericException("A fruit with the name '${name.get()}' already exists")
        }
    }

    @Transactional
    fun updateFruit(fruitUpdateDTO: FruitUpdateDTO): FruitResponseDTO {
        val id = fruitUpdateDTO.id ?: throw QueriedByEmptyIdException()

        val entityFromCache = fruitCache.getAllFruits().find { it.id == id } ?: throw ChangeSetPersister.NotFoundException()
        validateNameOnSave(fruitUpdateDTO.name, fruitUpdateDTO.id)
        fruitMapper.partialUpdate(fruitUpdateDTO, entityFromCache)
        fruitCache.upsertFruit(entityFromCache)

        return fruitMapper.toResponseDto(entityFromCache)
    }

    @Transactional
    fun deleteFruit(id: UUID?) {
        if (id != null) {
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

