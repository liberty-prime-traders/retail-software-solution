package me.ezra_home.retail_software_solution.organizations.business.jobtitle


import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.JobTitleEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TITLE])
internal class JobTitleCache(private val jobTitleRepository: JobTitleRepository) {

    @Cacheable
    fun getAllJobTitles(): Collection<JobTitleEntity> = jobTitleRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertJobTitle(jobTitleEntity: JobTitleEntity) {
        jobTitleRepository.save(jobTitleEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteJobTitle(id: UUID) {
        jobTitleRepository.deleteById(id)
    }
}
