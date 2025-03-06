package me.ezra_home.retail_software_solution.service

import me.ezra_home.retail_software_solution.exception.FruitNotFoundException
import me.ezra_home.retail_software_solution.model.dto.*
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import me.ezra_home.retail_software_solution.repository.FruitRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FruitService(private val fruitRepository: FruitRepository) {

    @Cacheable(value = ["fruits"], key = "#id")
    fun getFruitById(id: UUID): FruitResponseDTO {
        val fruit = fruitRepository.findById(id)
            .orElseThrow { FruitNotFoundException("Fruit with ID $id not found") }
        return fruit.toResponseDTO()
    }

    fun getFruitByName(name: String): FruitResponseDTO {
        val fruit = fruitRepository.findByName(name)
            ?: throw FruitNotFoundException("Fruit with name $name not found")
        return fruit.toResponseDTO()
    }

    @CacheEvict(value = ["fruits"], allEntries = true)
    fun createFruit(request: FruitRequestDTO): FruitResponseDTO {
        val fruit = fruitRepository.save(request.toEntity())
        return fruit.toResponseDTO()
    }

    @CacheEvict(value = ["fruits"], key = "#id")
    fun deleteFruit(id: UUID) {
        if (!fruitRepository.existsById(id)) {
            throw FruitNotFoundException("Fruit with ID $id not found")
        }
        fruitRepository.deleteById(id)
    }
}
