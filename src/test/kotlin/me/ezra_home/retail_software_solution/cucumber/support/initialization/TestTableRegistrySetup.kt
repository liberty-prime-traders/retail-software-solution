package me.ezra_home.retail_software_solution.cucumber.support.initialization

import jakarta.annotation.PostConstruct
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component(TestTableRegistrySetup.BEAN_NAME)
@DependsOn(DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE)
class TestTableRegistrySetup(private val tableRegistryCache: TableRegistryCache) {

  companion object {
    const val BEAN_NAME = "testTableRegistrySetup"
  }

  /***
   * Ensures that all tables in the registry are marked as validated before tests run.
   * This is necessary because every insert generates a reference# using a validated registry entry.
   * There is no need to test this because it is an infrastructure detail that end users will not
   * interact with directly, and a failure here would indicate a catastrophic issue in the system
   * that would be caught by other tests.
    */
  @PostConstruct
  fun validateAllTables() {
    tableRegistryCache.getAllTables()
      .filter { !it.validated }
      .forEach {
        it.validated = true
        tableRegistryCache.upsertTable(it)
      }
  }
}
