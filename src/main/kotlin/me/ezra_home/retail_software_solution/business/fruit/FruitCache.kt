package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import me.ezra_home.retail_software_solution.repository.FruitRepository
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheConfig(cacheNames = [CacheNames.FRUIT])
class FruitCache(private val fruitRepository: FruitRepository) {

    @Cacheable
    fun getAllFruits(): Collection<FruitEntity> = fruitRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertFruit(fruitEntity: FruitEntity) = fruitRepository.save(fruitEntity)

    @CacheEvict(allEntries = true)
    fun deleteFruit(id: UUID) {
        fruitRepository.deleteById(id)
    }
}
