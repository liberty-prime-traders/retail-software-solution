package me.ezra_home.retail_software_solution.locations.business.variation

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationInsertDto
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationResponseDto
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class VariationService(
    private val variationMapper: VariationMapper,
    private val variationCache: VariationCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllVariations(): Collection<VariationResponseDto> {
        return variationCache.getAllVariations().map { variationMapper.toResponseDto(it) }
    }

    fun createVariation(variationInsertDto: VariationInsertDto): VariationResponseDto {
        validateVariationInsert(variationInsertDto)
        val variationEntity = variationMapper.toEntity(variationInsertDto)
        variationCache.upsertVariation(variationEntity)
        return variationMapper.toResponseDto(variationEntity)
    }

    fun validateVariationInsert(variationInsertDto: VariationInsertDto) {
        val variationName = StringUtils.getValueOrException(variationInsertDto.name, NAME_IS_REQUIRED)
        variationCache.getAllVariations().find { StringUtils.isEquivalent(it.name, variationName) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, variationName)) }
    }

    fun updateVariation(variationUpdateDto: VariationUpdateDto): VariationResponseDto {
        validateVariationUpdate(variationUpdateDto)
        val existingVariation = variationCache.getAllVariations().find { it.id == variationUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()
        variationMapper.partialUpdate(variationUpdateDto, existingVariation)
        variationCache.upsertVariation(existingVariation)
        return variationMapper.toResponseDto(existingVariation)
    }

    fun validateVariationUpdate(variationUpdateDto: VariationUpdateDto) {
        val variationName = StringUtils.getValueOrException(variationUpdateDto.name, NAME_IS_REQUIRED)
        variationCache.getAllVariations()
            .find { StringUtils.isEquivalent(it.name, variationName) && it.id != variationUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, variationName)) }
    }

    fun deleteVariation(id: UUID?) {
        id?.let {
            variationCache.getAllVariations().find { it.id == id }?.let {entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Variation ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                variationCache.deleteVariation(id)
            }
        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A variation must have a name"
        const val NAME_ALREADY_EXISTS = "A variation with the name %s already exists"
    }
}
