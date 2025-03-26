package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.business.fruit.dto.FruitInsertDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitResponseDto
import me.ezra_home.retail_software_solution.business.fruit.dto.FruitUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class FruitService(
    private val fruitMapper: FruitMapper,
    private val fruitCache: FruitCache
) {

    @Transactional
    fun getAllFruits(): Collection<FruitResponseDto> {
        return fruitCache.getAllFruits().map { fruitMapper.toDto(it) }
    }
    @Transactional
    fun createFruit(fruitInsertDto: FruitInsertDto): FruitResponseDto {
        val fruitName :String = fruitInsertDto.name.takeIf { it.isNotBlank() }
            ?: throw RtsGenericException(NAME_IS_REQUIRED)

        if (fruitCache.getAllFruits().any { it.name.equals(fruitName, ignoreCase = true) }) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, fruitName))
        }
        val newFruitEntity = fruitMapper.toEntity(fruitInsertDto)
        val savedFruitEntity = fruitCache.upsertFruit(newFruitEntity)
        return fruitMapper.toDto(savedFruitEntity)
    }

    @Transactional
    fun updateFruit(fruitUpdateDto: FruitUpdateDto): FruitResponseDto {
        validateFruitUpdate(fruitUpdateDto)
        val fruitToUpdate = fruitCache.getAllFruits().find { it.id == fruitUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()

        fruitMapper.partialUpdate(fruitUpdateDto, fruitToUpdate)
        val updatedFruit = fruitCache.upsertFruit(fruitToUpdate)
        return fruitMapper.toDto(updatedFruit)
    }

    private fun validateFruitUpdate(fruitUpdateDto: FruitUpdateDto) {
        val name = fruitUpdateDto.name?.orElse(null)
            ?: throw RtsGenericException(NAME_IS_REQUIRED)

        if (fruitCache.getAllFruits().any {
                it.name.equals(name, ignoreCase = true) && it.id != fruitUpdateDto.id
            }) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
        }
    }

    @Transactional
    fun deleteFruit(id: UUID) {
        val entity = fruitCache.getAllFruits().find { it.id == id }
            ?: throw RtsGenericException("Fruit with ID $id not found")

        val usageCount = entity.usageCount
        if (usageCount > 0L) {
            throw RtsGenericException("Fruit ${entity.name} has $usageCount usage(s) and cannot be deleted")
        }
        fruitCache.deleteFruit(id)
    }

    companion object {
        const val NAME_IS_REQUIRED = "A fruit must have a name"
        const val NAME_ALREADY_EXISTS = "A fruit with the name %s is already assigned."
    }
}
