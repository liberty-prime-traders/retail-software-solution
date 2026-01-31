package me.ezra_home.retail_software_solution.locations.business.sync

sealed interface SyncCursor {
  data class Reference(val value: String) : SyncCursor
  data class Revision(val value: Long) : SyncCursor
}
