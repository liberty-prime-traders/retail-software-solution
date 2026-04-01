package me.ezra_home.retail_software_solution.locations.business.catalog_sync.dto

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncMode
import me.ezra_home.retail_software_solution.util.model.TableName
import java.io.Serializable

data class SyncRequestDto(
  val tableName: TableName,
  val syncMode: SyncMode
) : Serializable
