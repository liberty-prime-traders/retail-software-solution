package me.ezra_home.retail_software_solution.locations.business.prefix_sequence_tracker

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.rest.endpoints.prefix_sequence_tracker.LocationPrefixSequenceTrackerEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationPrefixSequenceTrackerService(
    private val locationPrefixSequenceTrackerCache: LocationPrefixSequenceTrackerCache
) {
    @TransactionalOnLocationSchema
    fun incrementNextNumber(tableRegistryId: UUID, prefix: String): Long {
        val tracker = locationPrefixSequenceTrackerCache.findByTableRegistryIdAndPrefix(tableRegistryId, prefix)
            ?: LocationPrefixSequenceTrackerEntity(tableRegistryId, prefix)
        val currentNumber = tracker.nextNumber
        tracker.nextNumber = currentNumber + 1
        locationPrefixSequenceTrackerCache.save(tracker)
        return currentNumber
    }
}
