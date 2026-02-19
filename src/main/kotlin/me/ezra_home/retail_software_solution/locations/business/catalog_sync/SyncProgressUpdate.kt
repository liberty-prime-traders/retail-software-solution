package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import java.util.UUID

data class SyncProgressUpdate(
  val syncLogId: UUID,
  val processed: Int,
  val skipped: Int,
  val failed: Int,
  val lastRevision: Long?
)
