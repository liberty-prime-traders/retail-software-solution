package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.platform.model.TableRegistryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface TableRegistryRepository: JpaRepository<TableRegistryEntity, UUID> {
    fun findByTableName(tableName: String): TableRegistryEntity?
}