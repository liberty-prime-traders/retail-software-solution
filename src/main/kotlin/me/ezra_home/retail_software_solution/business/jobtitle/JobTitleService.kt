package me.ezra_home.retail_software_solution.business.jobtitle

import java.util.Objects
import java.util.UUID
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class JobTitleService(
    private val titleMapper: JobTitleMapper,
    private val titleCache: JobTitleCache,
    private val jobTitleCache: JobTitleCache
) {

    @Transactional
    fun getAllJobTitles(): Collection<JobTitleResponseDto> {
        return titleCache.getAllJobTitles().map { titleMapper.toDto(it) }
    }

    @Transactional
    fun createJobTitle(titleInsertDto: JobTitleInsertDto): JobTitleResponseDto {
        val newTitleEntity = titleMapper.toEntity(titleInsertDto)
        val savedTitleEntity = titleCache.upsertJobTitles(newTitleEntity)
        return titleMapper.toDto(savedTitleEntity)
    }


    @Transactional
    fun updateJobTitle(titleDto: JobTitleUpdateDto): JobTitleResponseDto {
        validateJobTitleUpdate(titleDto)
        val titleToUpdate = titleCache.getAllJobTitles().find { Objects.equals(titleDto.id, it.id) }
        if (titleToUpdate == null) throw UpdatingNonExistingRecordException()
        titleMapper.partialUpdate(titleDto, titleToUpdate)
        val updatedTitle = titleCache.upsertJobTitles(titleToUpdate)
        return titleMapper.toDto(updatedTitle)
    }

    @Transactional
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