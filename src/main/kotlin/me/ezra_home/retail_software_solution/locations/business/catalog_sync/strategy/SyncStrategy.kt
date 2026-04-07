package me.ezra_home.retail_software_solution.locations.business.catalog_sync.strategy

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncCursor
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncMode

interface SyncStrategy {

  fun getSyncMode(): SyncMode

  fun shouldCalculateTotal(): Boolean

  fun createInitialCursor(resumableRevision: Long?): SyncCursor?

  fun getCursorForStorage(cursor: SyncCursor?): Long?
}
