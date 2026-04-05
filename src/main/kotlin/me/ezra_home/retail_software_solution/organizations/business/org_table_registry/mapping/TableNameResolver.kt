package me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping

import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class TableNameResolver(private val tableRegistryCache: TableRegistryCache) {

    @TableNameQualifier
    fun resolveTableName(registryId: UUID?): String? {
        return registryId?.let {
            tableRegistryCache.getAllTables().find { it.id == registryId }?.tableName
        }
    }
}
