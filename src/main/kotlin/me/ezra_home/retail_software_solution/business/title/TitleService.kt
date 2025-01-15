package me.ezra_home.retail_software_solution.business.title

import java.util.Objects
import me.ezra_home.retail_software_solution.business.title.dto.TitleInsertDto
import me.ezra_home.retail_software_solution.business.title.dto.TitleResponseDto
import me.ezra_home.retail_software_solution.business.title.dto.TitleUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class TitleService(
    private val titleMapper: TitleMapper,
    private val titleCache: TitleCache
) {

    @Transactional
    fun getAllTitles(): Collection<TitleResponseDto> {
        return titleCache.getAllTitles().map { titleMapper.toDto(it) }
    }

    @Transactional
    fun createTitle(titleInsertDto: TitleInsertDto): TitleResponseDto {
        val newTitleEntity = titleMapper.toEntity(titleInsertDto)
        val savedTitleEntity = titleCache.upsertTitles(newTitleEntity)
        return titleMapper.toDto(savedTitleEntity)
    }

    @Transactional
    fun updateTitle(titleDto: TitleUpdateDto): TitleResponseDto {
        val titleToUpdate = titleCache.getAllTitles().find { Objects.equals(titleDto.id, it.id) }
        if (titleToUpdate == null) throw UpdatingNonExistingRecordException()
        titleMapper.partialUpdate(titleDto, titleToUpdate)
        val updatedTitle = titleCache.upsertTitles(titleToUpdate)
        return titleMapper.toDto(updatedTitle)
    }
}