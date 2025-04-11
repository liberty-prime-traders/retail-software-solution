package me.ezra_home.retail_software_solution.business.variation

import jakarta.persistence.EntityNotFoundException
import me.ezra_home.retail_software_solution.business.variation.cache.VariationCache
import me.ezra_home.retail_software_solution.business.variation.dto.VariationInsertDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationResponseDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationUpdateDto
import org.springframework.stereotype.Service

@Service
class VariationService(
    private val variationRepository: VariationRepository,
    private val variationMapper: VariationMapper,
    private val variationCache: VariationCache
) {
    fun create(createDto: VariationInsertDto): VariationResponseDto {
        val entity = variationMapper.toEntity(createDto)
        val savedEntity = variationRepository.save(entity)
        return variationMapper.toDto(savedEntity)
    }

    fun findById(id: Long): VariationResponseDto {
        val entity = variationRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Variation not found with id: $id") }
        return variationMapper.toDto(entity)
    }

    fun update(id: Long, updateRequest: VariationUpdateDto): VariationResponseDto {
        val existingEntity = variationRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Variation not found with id: $id") }

        variationMapper.partialUpdate(updateRequest, existingEntity)
        val updatedEntity = variationRepository.save(existingEntity)
        return variationMapper.toDto(updatedEntity)
    }

    fun delete(id: Long) {
        if (!variationRepository.existsById(id)) {
            throw EntityNotFoundException("Variation not found with id: $id")
        }
        variationRepository.deleteById(id)
    }

    fun findAll(): List<VariationResponseDto> {
        return variationRepository.findAll()
            .map { variationMapper.toDto(it) }
    }
}
