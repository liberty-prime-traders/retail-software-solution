package me.ezra_home.retail_software_solution.organizations.business.org_expense.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class OrgExpenseSearchExecutor(
    @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    entityManagerFactory: LocalContainerEntityManagerFactoryBean
) : SqlSearchExecutor<Tuple, Tuple>(entityManagerFactory, Tuple::class.java) {

    override fun map(entities: List<Tuple>): List<Tuple> = entities
}
