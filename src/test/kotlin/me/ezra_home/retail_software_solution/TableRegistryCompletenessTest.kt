package me.ezra_home.retail_software_solution

import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.test.assertTrue

class TableRegistryCompletenessTest {

    private val registryEntriesDir =
        File("src/main/resources/db/changelog/platform/seeded_tables/registry_entries")

    @Test
    fun `every TableNames constant has a table_registry seed entry`() {
        val missing = tableNamesConstants() - registeredTableNames()
        assertTrue(
            missing.isEmpty(),
            "These tables are declared in TableNames but have no seed entry under registry_entries/: " +
                "${missing.sorted()}. Add a table_registry insert changeset for each (see rtss-db)."
        )
    }

    @Test
    fun `every table_registry seed entry still matches a declared TableNames constant`() {
        val orphaned = registeredTableNames() - tableNamesConstants()
        assertTrue(
            orphaned.isEmpty(),
            "These table_registry seed entries no longer match any TableNames constant " +
                "(stale rename or removed table?): ${orphaned.sorted()}"
        )
    }

    @Test
    fun `every TableNames constant has a matching TableName enum entry`() {
        val missing = tableNamesConstants() - tableNameEnumValues()
        assertTrue(
            missing.isEmpty(),
            "These tables are declared in TableNames but have no TableName enum entry: " +
                "${missing.sorted()}. Add an entry with the correct SchemaLevel (see rtss-db)."
        )
    }

    @Test
    fun `every TableName enum entry still matches a declared TableNames constant`() {
        val orphaned = tableNameEnumValues() - tableNamesConstants()
        assertTrue(
            orphaned.isEmpty(),
            "These TableName enum entries no longer match any TableNames constant " +
                "(stale rename or removed table?): ${orphaned.sorted()}"
        )
    }

    private fun tableNameEnumValues(): Set<String> =
        TableName.entries.map { it.code }.toSet()

    private fun tableNamesConstants(): Set<String> =
        TableNames::class.java.declaredFields
            .filter { it.type == String::class.java }
            .onEach { it.isAccessible = true }
            .map { it.get(null) as String }
            .toSet()

    @Suppress("UNCHECKED_CAST")
    private fun registeredTableNames(): Set<String> {
        val yaml = Yaml()
        return requireNotNull(registryEntriesDir.listFiles { file -> file.extension == "yml" }) {
            "Registry entries directory not found: $registryEntriesDir"
        }.mapNotNull { file -> extractTableName(yaml.load(file.inputStream())) }
            .toSet()
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractTableName(root: Map<String, Any>): String? {
        val changeLogEntries = root["databaseChangeLog"] as List<Map<String, Any>>
        return changeLogEntries.firstNotNullOfOrNull { entry ->
            val changeSet = entry["changeSet"] as? Map<String, Any> ?: return@firstNotNullOfOrNull null
            val changes = changeSet["changes"] as List<Map<String, Any>>
            changes.firstNotNullOfOrNull { change ->
                val insert = change["insert"] as? Map<String, Any> ?: return@firstNotNullOfOrNull null
                val columns = insert["columns"] as List<Map<String, Any>>
                columns.firstNotNullOfOrNull { columnWrapper ->
                    val column = columnWrapper["column"] as Map<String, Any>
                    (column["value"] as? String)?.takeIf { column["name"] == "table_name" }
                }
            }
        }
    }
}
