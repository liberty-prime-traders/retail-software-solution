package me.ezra_home.retail_software_solution.business.variation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.List

@Repository
interface VariationRepository : JpaRepository<VariationEntity, UUID> {
    fun findByIsActiveTrue(): List<VariationEntity>
    fun findByVariationIdAndIsActiveTrue(variationId: UUID): VariationEntity?
}