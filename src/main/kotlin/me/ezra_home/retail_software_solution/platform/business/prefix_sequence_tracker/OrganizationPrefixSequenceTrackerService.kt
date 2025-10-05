package me.ezra_home.retail_software_solution.platform.business.prefix_sequence_tracker

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationPrefixSequenceTrackerService(
    private val prefixSequenceTrackerCache: OrganizationPrefixSequenceTrackerCache
) {
    @TransactionalOnOrganizationSchema
    fun incrementNextNumber(tableRegistryId: UUID, prefix: String): Long {
        val tracker = prefixSequenceTrackerCache.findByTableRegistryIdAndPrefix(tableRegistryId, prefix)
            ?: OrganizationPrefixSequenceTrackerEntity(tableRegistryId, prefix)
        val currentNumber = tracker.nextNumber
        tracker.nextNumber = currentNumber + 1
        prefixSequenceTrackerCache.save(tracker)
        return currentNumber
    }
}
