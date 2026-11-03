package me.ezra_home.retail_software_solution.organizations.business.manual_journal

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ManualJournalLineRepository : JpaRepository<ManualJournalLineEntity, UUID> {

    fun findByManualJournalIdIn(manualJournalIds: Collection<UUID>): List<ManualJournalLineEntity>
}
