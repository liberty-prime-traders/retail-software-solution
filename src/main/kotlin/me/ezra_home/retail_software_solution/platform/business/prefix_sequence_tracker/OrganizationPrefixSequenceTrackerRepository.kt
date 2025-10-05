package me.ezra_home.retail_software_solution.platform.business.prefix_sequence_tracker

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrganizationPrefixSequenceTrackerRepository : JpaRepository<OrganizationPrefixSequenceTrackerEntity, UUID> {
    fun findByTableRegistryIdAndPrefix(tableRegistryId: UUID, prefix: String): OrganizationPrefixSequenceTrackerEntity?
}
