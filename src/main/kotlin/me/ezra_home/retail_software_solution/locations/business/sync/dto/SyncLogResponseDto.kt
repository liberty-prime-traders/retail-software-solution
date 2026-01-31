package me.ezra_home.retail_software_solution.locations.business.sync.dto

import me.ezra_home.retail_software_solution.util.enums.SyncMode
import me.ezra_home.retail_software_solution.util.enums.SyncStatus
import me.ezra_home.retail_software_solution.util.model.TableName
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class SyncLogResponseDto(
  val id: UUID?,
  val tableName: TableName,
  val syncMode: SyncMode,
  val status: SyncStatus,
  val totalRecords: Int?,
  val processedRecords: Int,
  val skippedRecords: Int,
  val failedRecords: Int,
  val percentComplete: Double?,
  val lastProcessedRevision: Long?,
  val createdOn: OffsetDateTime?,
  val startedAt: OffsetDateTime?,
  val completedAt: OffsetDateTime? = null,
  val canceledAt: OffsetDateTime? = null,
  val errorMessage: String? = null
) : Serializable
