package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LedgerEntryGroupRepository : JpaRepository<LedgerEntryGroupEntity, UUID> {
    fun findByReferenceNumberIn(referenceNumbers: Set<String>): List<LedgerEntryGroupEntity>
    fun findBySourceReferenceNumberInAndSourceTypeAndSourceLocationId(
        references: Collection<String>, sourceType: LedgerSourceType, locationId: UUID
    ): List<LedgerEntryGroupEntity>
    fun existsBySourceReferenceNumberAndSourceLocationId(reference: String, locationId: UUID): Boolean
    fun existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(reference: String, sourceType: LedgerSourceType, locationId: UUID): Boolean
    fun existsBySourceReferenceNumberAndSourceTypeAndSourceLocationIdIsNull(reference: String, sourceType: LedgerSourceType): Boolean
}
