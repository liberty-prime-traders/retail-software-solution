package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.service.OrganizationReferenceNumberGeneratorService
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID


@Service
@TransactionalOnOrganizationSchema
class JobTitleService(
    private val jobTitleMapper: JobTitleMapper,
    private val jobTitleCache: JobTitleCache,
    private val referenceNumberGeneratorService: OrganizationReferenceNumberGeneratorService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllJobTitles(): Collection<JobTitleResponseDto> {
        return jobTitleCache.getAllJobTitles().map { jobTitleMapper.toDto(it) }
    }

    fun createJobTitle(titleInsertDto: JobTitleInsertDto): JobTitleResponseDto {
        val value = StringUtils.getValueOrException(titleInsertDto.value, VALUE_IS_REQUIRED)
        jobTitleCache.getAllJobTitles().find { StringUtils.isEquivalent(it.value, value) }
            ?.let { throw RtsGenericException(String.format(VALUE_ALREADY_EXISTS, value)) }

        val newTitleEntity = jobTitleMapper.toEntity(titleInsertDto)
        newTitleEntity.referenceNumber = referenceNumberGeneratorService.generateReferenceNumber(TableNames.JOB_TITLE)
        jobTitleCache.upsertJobTitle(newTitleEntity)
        return jobTitleMapper.toDto(newTitleEntity)
    }

    fun updateJobTitle(titleDto: JobTitleUpdateDto): JobTitleResponseDto {
        validateJobTitleUpdate(titleDto)
        val titleToUpdate = jobTitleCache.getAllJobTitles().find { Objects.equals(titleDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        jobTitleMapper.partialUpdate(titleDto, titleToUpdate)
        jobTitleCache.upsertJobTitle(titleToUpdate)
        return jobTitleMapper.toDto(titleToUpdate)
    }

    private fun validateJobTitleUpdate(jobTitleUpdateDto: JobTitleUpdateDto) {
        val value = StringUtils.getValueOrException(jobTitleUpdateDto.value, VALUE_IS_REQUIRED)
        jobTitleCache.getAllJobTitles().find { it.value.equals(value, ignoreCase = true) && it.id != jobTitleUpdateDto.id }
            ?.let{ throw RtsGenericException(String.format(VALUE_ALREADY_EXISTS, value)) }
    }

    fun deleteJobTitle(id: UUID?) {
        id?.let {
            jobTitleCache.getAllJobTitles().find { it.id == id }?.let { entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Job Title ${entity.value} has $usageCount usage(s) and cannot be deleted")
                }
                jobTitleCache.deleteJobTitle(id)
            }
        }
    }

    companion object {
        const val VALUE_IS_REQUIRED = "A job title must have a value"
        const val VALUE_ALREADY_EXISTS = "A job title with the value %s already exists."
    }

}
