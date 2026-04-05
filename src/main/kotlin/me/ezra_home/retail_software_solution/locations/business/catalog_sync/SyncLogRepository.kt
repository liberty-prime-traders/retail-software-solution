package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncLogEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SyncLogRepository : JpaRepository<SyncLogEntity, UUID> {

  @Query("""
    SELECT s.lastProcessedRevision
    FROM SyncLogEntity s
    WHERE s.tableName = :tableName
    AND s.status = me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.COMPLETED
    AND s.lastProcessedRevision IS NOT NULL
    ORDER BY s.completedAt DESC
    LIMIT 1
  """)
  fun findLastSuccessfulSyncRevision(tableName: TableName): Long?

  @Query("""
    SELECT s
    FROM SyncLogEntity s
    WHERE s.tableName = :tableName
    AND s.status IN (
      me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.IN_PROGRESS,
      me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.CANCELLATION_REQUESTED
    )
    ORDER BY s.createdOn DESC
    LIMIT 1
  """)
  fun findInProgressSync(tableName: TableName): SyncLogEntity?

  @Query("""
    SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
    FROM SyncLogEntity s
    WHERE s.tableName = :tableName
    AND s.status IN (
      me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.IN_PROGRESS,
      me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.CANCELLATION_REQUESTED
    )
  """)
  fun existsInProgressSync(tableName: TableName): Boolean

  @Query("""
    SELECT s.lastProcessedRevision
    FROM SyncLogEntity s
    WHERE s.tableName = :tableName
    AND s.status = me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.FAILED
    AND s.lastProcessedRevision IS NOT NULL
    ORDER BY s.completedAt DESC
    LIMIT 1
  """)
  fun findLastFailedSyncRevision(tableName: TableName): Long?

  @Query("""
    SELECT s.lastProcessedRevision
    FROM SyncLogEntity s
    WHERE s.tableName = :tableName
    AND s.status = me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus.CANCELLED
    AND s.lastProcessedRevision IS NOT NULL
    ORDER BY s.canceledAt DESC
    LIMIT 1
  """)
  fun findLastCancelledSyncRevision(tableName: TableName): Long?

  @Query("""
    SELECT CASE WHEN s.canceledAt IS NOT NULL THEN true ELSE false END
    FROM SyncLogEntity s
    WHERE s.id = :syncLogId
  """)
  fun isCancelled(syncLogId: UUID): Boolean

  @Query("""
    SELECT s
    FROM SyncLogEntity s
    ORDER BY s.createdOn DESC
    LIMIT :limit
  """)
  fun findTopN(limit: Int): List<SyncLogEntity>
}
