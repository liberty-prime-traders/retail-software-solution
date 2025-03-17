package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.model.dto.FruitRequestDTO
import me.ezra_home.retail_software_solution.model.dto.FruitResponseDTO
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FruitService(private val fruitCache: FruitCache) {

    fun getFruitById(id: UUID): FruitResponseDTO {
        return fruitCache.getFruitById(id)
    }

    fun getFruitByName(name: String): FruitResponseDTO {
        return fruitCache.getFruitByName(name)
    }

    fun createFruit(request: FruitRequestDTO): FruitResponseDTO {
        return fruitCache.saveFruit(request)
    }

    fun deleteFruit(id: UUID) {
        fruitCache.deleteFruit(id)
    }
}
