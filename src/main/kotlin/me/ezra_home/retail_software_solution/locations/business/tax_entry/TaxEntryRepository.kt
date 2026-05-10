package me.ezra_home.retail_software_solution.locations.business.tax_entry

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaxEntryRepository : JpaRepository<TaxEntryEntity, UUID> {

    fun existsBySourceReferenceNumberAndSourceType(
        sourceReferenceNumber: String,
        sourceType: TaxSourceType
    ): Boolean

    fun findBySourceReferenceNumberAndSourceType(
        sourceReferenceNumber: String,
        sourceType: TaxSourceType
    ): List<TaxEntryEntity>
}
