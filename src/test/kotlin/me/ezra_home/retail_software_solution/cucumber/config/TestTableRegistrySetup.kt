package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.annotation.PostConstruct
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component
@DependsOn(DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE)
class TestTableRegistrySetup(private val tableRegistryCache: TableRegistryCache) {

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
