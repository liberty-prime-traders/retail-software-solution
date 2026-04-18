package me.ezra_home.retail_software_solution.organizations.business.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LedgerEntryGroupRepository : JpaRepository<LedgerEntryGroupEntity, UUID> {
    fun existsBySourceReferenceNumber(reference: String): Boolean
    fun existsBySourceReferenceNumberAndSourceType(reference: String, sourceType: LedgerSourceType): Boolean
}
