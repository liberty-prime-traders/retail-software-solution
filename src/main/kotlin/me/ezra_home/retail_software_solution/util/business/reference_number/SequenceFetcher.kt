package me.ezra_home.retail_software_solution.util.business.reference_number

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service


@Service
class SequenceFetcher(
    @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    private final val organizationEntityManagerFactory: EntityManagerFactory,

    @param:Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_ENTITY_MANAGER_FACTORY)
    private final var platformEntityManagerFactory: EntityManagerFactory,

    @param:Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    private final var locationEntityManagerFactory: EntityManagerFactory
) {

    fun getNextSequenceValue(tableName: TableName, schemaLevel: SchemaLevel): String {
        val sequenceName = generateSequenceName(tableName)
        return withEntityManager(schemaLevel) { entityManager ->
            entityManager.createNativeQuery("SELECT nextval(:sequenceName)")
                .setParameter("sequenceName", sequenceName)
                .singleResult.toString()
        }
    }

    fun getBulkSequenceValues(tableName: TableName, schemaLevel: SchemaLevel, count: Int): List<String> {
        val sequenceName = generateSequenceName(tableName)
        val procedureName = "get_next_sequence_values"
        @Suppress("UNCHECKED_CAST")
        return withEntityManager(schemaLevel) { entityManager ->
            entityManager.createNativeQuery("SELECT * FROM $procedureName(:sequenceName, :count)")
            .setParameter("sequenceName", sequenceName)
            .setParameter("count", count)
            .resultList as List<String>
        }
    }

    private fun generateSequenceName(tableName: TableName): String {
        return "seq_${tableName.tableName}_reference_number"
    }

    private fun <T> withEntityManager(schemaLevel: SchemaLevel, block: (EntityManager) -> T): T {
        return getEntityManager(schemaLevel).use(block)
    }

    private fun getEntityManager(schemaLevel: SchemaLevel): EntityManager {
        return when (schemaLevel) {
            SchemaLevel.PLATFORM -> platformEntityManagerFactory.createEntityManager()
            SchemaLevel.ORGANIZATION  -> organizationEntityManagerFactory.createEntityManager()
            SchemaLevel.LOCATION -> locationEntityManagerFactory.createEntityManager()
        }
    }

}
