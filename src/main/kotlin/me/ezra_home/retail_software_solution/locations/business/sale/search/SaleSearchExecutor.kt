package me.ezra_home.retail_software_solution.locations.business.sale.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class SaleSearchExecutor(
  @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean
) : SqlSearchExecutor<Tuple, Tuple>(emf, Tuple::class.java) {

  override fun map(entities: List<Tuple>): List<Tuple> = entities
}
