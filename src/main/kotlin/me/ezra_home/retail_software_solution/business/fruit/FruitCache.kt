package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.*

@CacheConfig(cacheNames = ["fruits"])
@Component
class FruitCache(private val fruitRepository: FruitRepository) {

    @Cacheable
    fun getAllFruits(): Collection<FruitEntity> {
        return fruitRepository.findAll() // Fetches all fruits from the database
    }

    @CacheEvict(allEntries = true)
    fun upsertFruit(fruitEntity: FruitEntity) {
        fruitRepository.save(fruitEntity) // Saves the fruit entity to the database
    }

    @CacheEvict(allEntries = true)
    fun deleteFruit(id: UUID) {
        fruitRepository.deleteById(id) // Deletes the fruit entity from the database
    }
}
