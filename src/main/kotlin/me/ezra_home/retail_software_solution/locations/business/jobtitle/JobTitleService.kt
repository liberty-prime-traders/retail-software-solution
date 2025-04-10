package me.ezra_home.retail_software_solution.locations.business.jobtitle

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.locations.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.locations.business.jobtitle.dto.JobTitleUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID


@Service
@TransactionalOnLocationSchema
class JobTitleService(
    private val jobTitleMapper: JobTitleMapper,
    private val jobTitleCache: JobTitleCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllJobTitles(): Collection<JobTitleResponseDto> {
        return jobTitleCache.getAllJobTitles().map { jobTitleMapper.toDto(it) }
    }

    fun createJobTitle(titleInsertDto: JobTitleInsertDto): JobTitleResponseDto {
        val value = titleInsertDto.value?.takeIf { it.isNotBlank() }
            ?: throw RtsGenericException(VALUE_IS_REQUIRED)

        if (jobTitleCache.getAllJobTitles().any { it.value.equals(value, ignoreCase = true) }) {
            throw RtsGenericException(String.format(VALUE_ALREADY_EXISTS, value))
        }

        val newTitleEntity = jobTitleMapper.toEntity(titleInsertDto)
        val savedTitleEntity = jobTitleCache.upsertJobTitles(newTitleEntity)
        return jobTitleMapper.toDto(savedTitleEntity)
    }

    fun updateJobTitle(titleDto: JobTitleUpdateDto): JobTitleResponseDto {
        validateJobTitleUpdate(titleDto)
        val titleToUpdate = jobTitleCache.getAllJobTitles().find { Objects.equals(titleDto.id, it.id) }
        if (titleToUpdate == null) throw UpdatingNonExistingRecordException()
        jobTitleMapper.partialUpdate(titleDto, titleToUpdate)
        val updatedTitle = jobTitleCache.upsertJobTitles(titleToUpdate)
        return jobTitleMapper.toDto(updatedTitle)
    }

    fun deleteJobTitle(id: UUID?) {
        if (id != null) {
            val entity = jobTitleCache.getAllJobTitles().find { it.id == id }
            if (entity != null) {
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Job Title ${entity.value} has $usageCount usage(s) and cannot be deleted")
                }
                jobTitleCache.deleteJobTitle(id)
            }
        }
    }
    
    private fun validateJobTitleUpdate(jobTitleUpdateDto: JobTitleUpdateDto) {
        val value = jobTitleUpdateDto.value?.orElse(null)
            ?: throw RtsGenericException(VALUE_IS_REQUIRED)

        if (jobTitleCache.getAllJobTitles().any {
                it.value.equals(value, ignoreCase = true) && it.id != jobTitleUpdateDto.id
            }) {
            throw RtsGenericException(String.format(VALUE_ALREADY_EXISTS, value))
        }
    }

    companion object {
        const val VALUE_IS_REQUIRED = "A value must have a name"
        const val VALUE_ALREADY_EXISTS = "A value with the name %s is already assigned."
    }

}
