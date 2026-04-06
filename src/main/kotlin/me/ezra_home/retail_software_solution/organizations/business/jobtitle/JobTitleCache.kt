package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TITLE])
class JobTitleCache(
    private val jobTitleRepository: JobTitleRepository,
    private val jobTitleMapper: JobTitleMapper
) {

    @Cacheable
    fun getAllJobTitles(): Collection<JobTitleDto> = jobTitleRepository.findAll().map { jobTitleMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: JobTitleInsertDto): JobTitleDto {
        val saved = jobTitleRepository.saveAndFlush(jobTitleMapper.toEntity(insertDto))
        return jobTitleMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(jobTitleDto: JobTitleDto): JobTitleDto {
        val saved = jobTitleRepository.save(jobTitleMapper.toEntity(jobTitleDto))
        return jobTitleMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteJobTitle(id: UUID) {
        jobTitleRepository.deleteById(id)
    }
}
