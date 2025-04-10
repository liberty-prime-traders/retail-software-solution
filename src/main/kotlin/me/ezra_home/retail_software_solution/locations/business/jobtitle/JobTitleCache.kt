package me.ezra_home.retail_software_solution.locations.business.jobtitle


import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.locations.model.JobTitleEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheNames = [CacheNames.TITLE])
class JobTitleCache(private val titleRepository: JobTitleRepository) {

    @Cacheable
    fun getAllJobTitles(): Collection<JobTitleEntity> = titleRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertJobTitles(titleEntity: JobTitleEntity): JobTitleEntity = titleRepository.save(titleEntity)

    @CacheEvict(allEntries = true)
    fun deleteJobTitle(id: UUID) {
        titleRepository.deleteById(id)
    }
}
