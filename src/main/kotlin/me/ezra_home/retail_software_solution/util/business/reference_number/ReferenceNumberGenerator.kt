package me.ezra_home.retail_software_solution.util.business.reference_number

import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.OrgTableRegistryCache
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service

@Service
class ReferenceNumberGenerator(
    private val sequenceFetcher: SequenceFetcher,
    private val tableRegistryCache: TableRegistryCache,
    private val orgTableRegistryCache: OrgTableRegistryCache
) {

    companion object {
        const val REFERENCE_NUMBER_MIN_LENGTH = 6
    }
    fun generateSingle(tableName: TableName): String {
        return generateBulk(tableName, 1).first()
    }

    fun generateBulk(tableName: TableName, count: Int): List<String> {
        require(count > 0) { "Count must be greater than 0" }
        val schemaLevel = getSchemaLevel(tableName)
        val sequenceNumbers = if (count == 1) {
            listOf(sequenceFetcher.getNextSequenceValue(tableName, schemaLevel))
        } else {
            sequenceFetcher.getBulkSequenceValues(tableName, schemaLevel, count)
        }
        val prefix = getPrefix(tableName, schemaLevel)
        return sequenceNumbers.map { prefix.padEnd(REFERENCE_NUMBER_MIN_LENGTH, '0') + it }
    }

    private fun getSchemaLevel(tableName: TableName): SchemaLevel {
        val table = tableRegistryCache.getAllTables()
            .firstOrNull { it.tableName == tableName.tableName }
            ?: throw RtsGenericException("Table ${tableName.tableName} not found in registry")

        return table.schemaLevel
    }

    private fun getPrefix(tableName: TableName, schemaLevel: SchemaLevel): String {
        val platformTable = tableRegistryCache.getAllTables()
            .firstOrNull { it.tableName == tableName.tableName }
            ?: throw RtsGenericException("Table ${tableName.tableName} not found in platform registry")

        if (!platformTable.validated) {
            throw RtsGenericException("Table ${tableName.tableName} is not validated hence cannot generate reference number")
        }
        if (schemaLevel != SchemaLevel.PLATFORM) {
            orgTableRegistryCache.getAllTables()
                .firstOrNull { it.registryId == platformTable.id }
                ?.apply { return this.defaultPrefix }
        }

        return platformTable.defaultPrefix
    }

}
