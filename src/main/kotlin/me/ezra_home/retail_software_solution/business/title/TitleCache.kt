package me.ezra_home.retail_software_solution.business.title


import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.TitleEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheNames = [CacheNames.TITLE])
class TitleCache(private val titleRepository: TitleRepository) {

    @Cacheable
    fun getAllTitles(): Collection<TitleEntity> = titleRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertTitles(titleEntity: TitleEntity): TitleEntity = titleRepository.save(titleEntity)
}