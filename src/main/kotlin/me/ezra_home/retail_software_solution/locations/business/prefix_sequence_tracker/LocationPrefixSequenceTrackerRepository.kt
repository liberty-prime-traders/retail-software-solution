package me.ezra_home.retail_software_solution.locations.business.prefix_sequence_tracker

import me.ezra_home.retail_software_solution.locations.rest.endpoints.prefix_sequence_tracker.LocationPrefixSequenceTrackerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationPrefixSequenceTrackerRepository : JpaRepository<LocationPrefixSequenceTrackerEntity, UUID> {
    fun findByTableRegistryIdAndPrefix(tableRegistryId: UUID, prefix: String): LocationPrefixSequenceTrackerEntity?
}
