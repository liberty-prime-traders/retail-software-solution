package me.ezra_home.retail_software_solution.locations.business.catalog_sync.strategy

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncCursor
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncMode
import org.springframework.stereotype.Component

@Component
class FullSyncStrategy : SyncStrategy {

  override fun getSyncMode(): SyncMode = SyncMode.FULL

  override fun shouldCalculateTotal(): Boolean = true

  override fun createInitialCursor(resumableRevision: Long?): SyncCursor? = null

  override fun getCursorForStorage(cursor: SyncCursor?): Long? = null
}
