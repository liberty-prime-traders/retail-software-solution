package me.ezra_home.retail_software_solution.util.service

import me.ezra_home.retail_software_solution.organizations.business.prefix_configuration.OrganizationPrefixConfigurationService
import me.ezra_home.retail_software_solution.platform.business.prefix_sequence_tracker.OrganizationPrefixSequenceTrackerService
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class OrganizationReferenceNumberGeneratorService(
    private val organizationPrefixConfigurationService: OrganizationPrefixConfigurationService,
    private val prefixSequenceTrackerService: OrganizationPrefixSequenceTrackerService,
    private val tableRegistryService: TableRegistryService
) {
    fun generateReferenceNumber(tableName: String): String {
        val orgPrefixConfig = organizationPrefixConfigurationService.getPrefixConfigurationForTable(tableName)
        val tableRegistry = tableRegistryService.getTableRegistryForTableName(tableName)
        val prefix = orgPrefixConfig?.prefix ?: tableRegistry.defaultPrefix
            ?: throw RtsGenericException("No prefix available for table: $tableName")
        val nextNumber = prefixSequenceTrackerService.incrementNextNumber(tableRegistry.id!!, prefix)
        return "%s%03d".format(prefix, nextNumber)
    }
}
