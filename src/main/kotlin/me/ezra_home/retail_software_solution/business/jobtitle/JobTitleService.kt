package me.ezra_home.retail_software_solution.business.jobtitle

import java.util.Objects
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class JobTitleService(
    private val titleMapper: JobTitleMapper,
    private val titleCache: JobTitleCache
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
        val titleToUpdate = titleCache.getAllJobTitles().find { Objects.equals(titleDto.id, it.id) }
        if (titleToUpdate == null) throw UpdatingNonExistingRecordException()
        titleMapper.partialUpdate(titleDto, titleToUpdate)
        val updatedTitle = titleCache.upsertJobTitles(titleToUpdate)
        return titleMapper.toDto(updatedTitle)
    }
}