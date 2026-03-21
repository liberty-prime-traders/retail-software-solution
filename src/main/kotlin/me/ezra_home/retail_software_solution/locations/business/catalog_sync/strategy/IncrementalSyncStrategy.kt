package me.ezra_home.retail_software_solution.locations.business.catalog_sync.strategy

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncCursor
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncMode
import org.springframework.stereotype.Component

@Component
class IncrementalSyncStrategy : SyncStrategy {

  override fun getSyncMode(): SyncMode = SyncMode.INCREMENTAL

  override fun shouldCalculateTotal(): Boolean = false

  override fun createInitialCursor(resumableRevision: Long?): SyncCursor? {
    return SyncCursor.Revision(resumableRevision ?: 0L)
  }

  override fun getCursorForStorage(cursor: SyncCursor?): Long? {
    return (cursor as? SyncCursor.Revision)?.value
  }
}
