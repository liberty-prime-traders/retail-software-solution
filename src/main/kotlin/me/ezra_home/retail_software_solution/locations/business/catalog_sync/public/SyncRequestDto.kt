package me.ezra_home.retail_software_solution.locations.business.catalog_sync.public

import me.ezra_home.retail_software_solution.util.model.TableName
import java.io.Serializable

data class SyncRequestDto(
  val tableName: TableName,
  val syncMode: SyncMode
) : Serializable
