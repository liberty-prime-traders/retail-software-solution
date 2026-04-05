package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TITLE])
internal class JobTitleCache(
    private val jobTitleRepository: JobTitleRepository,
    private val jobTitleMapper: JobTitleMapper
) {

    @Cacheable
    fun getAllJobTitles(): Collection<JobTitleDto> = jobTitleRepository.findAll().map { jobTitleMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsertJobTitle(jobTitleDto: JobTitleDto) {
        jobTitleRepository.save(jobTitleMapper.toEntity(jobTitleDto))
    }

    @CacheEvict(allEntries = true)
    fun deleteJobTitle(id: UUID) {
        jobTitleRepository.deleteById(id)
    }
}
