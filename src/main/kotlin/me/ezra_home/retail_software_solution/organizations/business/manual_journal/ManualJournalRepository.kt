package me.ezra_home.retail_software_solution.organizations.business.manual_journal

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ManualJournalRepository : JpaRepository<ManualJournalEntity, UUID> {

    fun findByReferenceNumber(referenceNumber: String): ManualJournalEntity?

    fun existsByReversalOfManualJournalId(reversalOfManualJournalId: UUID): Boolean

    fun findTop500ByOrderByCreatedOnDesc(): List<ManualJournalEntity>
}
