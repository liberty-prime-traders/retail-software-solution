package me.ezra_home.retail_software_solution.organizations.business.jobtitle.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleCache
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID


@Service
@TransactionalOnOrganizationSchema
class JobTitleService(
    private val jobTitleMapper: JobTitleMapper,
    private val jobTitleCache: JobTitleCache,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllJobTitles(): Collection<JobTitleResponseDto> {
        return jobTitleCache.getAllJobTitles().map { jobTitleMapper.toDto(it) }
    }

    fun createJobTitle(titleInsertDto: JobTitleInsertDto): JobTitleResponseDto {
        val value = StringUtils.getValueOrException(titleInsertDto.value, VALUE_IS_REQUIRED)
        jobTitleCache.getAllJobTitles().find { StringUtils.isEquivalent(it.value, value) }
            ?.let { throw RtsGenericException(String.format(VALUE_ALREADY_EXISTS, value)) }

        val dto = jobTitleMapper.toDomainDto(titleInsertDto)
        jobTitleCache.upsertJobTitle(dto)
        return jobTitleMapper.toDto(dto)
    }

    fun updateJobTitle(titleDto: JobTitleUpdateDto): JobTitleResponseDto {
        validateJobTitleUpdate(titleDto)
        val dto = jobTitleCache.getAllJobTitles().find { Objects.equals(titleDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        jobTitleMapper.partialUpdate(titleDto, dto)
        jobTitleCache.upsertJobTitle(dto)
        return jobTitleMapper.toDto(dto)
    }

    private fun validateJobTitleUpdate(jobTitleUpdateDto: JobTitleUpdateDto) {
        val value = StringUtils.getValueOrException(jobTitleUpdateDto.value, VALUE_IS_REQUIRED)
        jobTitleCache.getAllJobTitles().find { it.value.equals(value, ignoreCase = true) && it.id != jobTitleUpdateDto.id }
            ?.let{ throw RtsGenericException(String.format(VALUE_ALREADY_EXISTS, value)) }
    }

    fun deleteJobTitle(id: UUID) {
        jobTitleCache.deleteJobTitle(id)
    }

    companion object {
        const val VALUE_IS_REQUIRED = "A job title must have a value"
        const val VALUE_ALREADY_EXISTS = "A job title with the value %s already exists."
    }

}
