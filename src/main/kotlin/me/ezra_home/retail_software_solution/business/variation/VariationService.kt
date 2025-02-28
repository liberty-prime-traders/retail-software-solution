// business/variation/VariationService.kt
package me.ezra_home.retail_software_solution.business.variation

import me.ezra_home.retail_software_solution.business.variation.dto.CreateVariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.UpdateVariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationEntityDto
import me.ezra_home.retail_software_solution.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class VariationService(
    private val variationRepository: VariationRepository,
    private val variationEntityMapper: VariationEntityMapper,
    private val variationCache: VariationCache
) {
    fun getAllVariations() {
        val variations = variationCache.getAllVariations()
        return variationEntityMapper.toDtoList(variations)
    }

    fun getVariation(variationId: UUID): VariationEntityDto {
        val variation = variationCache.getVariation(variationId)
            ?: throw ResourceNotFoundException("Variation not found with ID: $variationId")
        return variationEntityMapper.toDto(variation)
    }

    @Transactional
    fun createVariation(dto: CreateVariationDto, userId: UUID): VariationEntityDto {
        var variationEntity = variationEntityMapper.toEntity(dto)
        variationEntity = variationEntity.copy(createdBy = userId)
        val savedVariation = variationRepository.save(variationEntity)
        variationCache.addToCache(savedVariation)
        return variationEntityMapper.toDto(savedVariation)
    }

    @Transactional
    fun updateVariation(variationId: UUID, dto: UpdateVariationDto, userId: UUID): VariationEntityDto {
        val variation = variationCache.getVariation(variationId)
            ?: throw ResourceNotFoundException("Variation not found with ID: $variationId")

        dto.name?.let { variation.name = it }
        dto.description?.let { variation.description = it }
        variation.updatedAt = LocalDateTime.now()
        variation.updatedBy = userId

        val updatedVariation = variationRepository.save(variation)
        variationCache.invalidateCache(variationId)
        variationCache.addToCache(updatedVariation)
        return variationEntityMapper.toDto(updatedVariation)
    }

    @Transactional
    fun deleteVariation(variationId: UUID, userId: UUID) {
        val variation = variationCache.getVariation(variationId)
            ?: throw ResourceNotFoundException("Variation not found with ID: $variationId")

        variation.isActive = false
        variation.updatedAt = LocalDateTime.now()
        variation.updatedBy = userId

        variationRepository.save(variation)
        variationCache.invalidateCache(variationId)
    }
}