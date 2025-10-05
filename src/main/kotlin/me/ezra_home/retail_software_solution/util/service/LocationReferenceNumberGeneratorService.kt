package me.ezra_home.retail_software_solution.util.service

import me.ezra_home.retail_software_solution.locations.business.prefix_configuration.LocationPrefixConfigurationService
import me.ezra_home.retail_software_solution.locations.business.prefix_sequence_tracker.LocationPrefixSequenceTrackerService
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class LocationReferenceNumberGeneratorService(
    private val locationPrefixConfigurationService: LocationPrefixConfigurationService,
    private val locationPrefixSequenceTrackerService: LocationPrefixSequenceTrackerService,
    private val tableRegistryService: TableRegistryService
) {
    fun generateReferenceNumber(tableName: String): String {
        val locationPrefixConfig = locationPrefixConfigurationService.getPrefixConfigurationForTable(tableName)
        val tableRegistry = tableRegistryService.getTableRegistryForTableName(tableName)
        val prefix = locationPrefixConfig?.prefix ?: tableRegistry.defaultPrefix
            ?: throw RtsGenericException("No prefix available for table: $tableName")
        val nextNumber = locationPrefixSequenceTrackerService.incrementNextNumber(tableRegistry.id!!, prefix)
        return "%s%03d".format(prefix, nextNumber)
    }
}
