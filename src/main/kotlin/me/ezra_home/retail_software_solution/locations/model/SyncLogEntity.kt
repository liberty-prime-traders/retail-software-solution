package me.ezra_home.retail_software_solution.locations.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncInitiateType
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncInitiateTypeConverter
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncMode
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncModeConverter
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncStatus
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncStatusConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNameConverter
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime

@Entity
@Table(name = TableNames.SYNC_LOG)
@HasReference(tableName = TableName.SYNC_LOG)
internal class SyncLogEntity(

  @Convert(converter = TableNameConverter::class)
  @Column(name = "table_name", nullable = false, length = 50)
  var tableName: TableName,

  @Convert(converter = SyncModeConverter::class)
  @Column(name = "sync_mode", nullable = false, length = 5)
  var syncMode: SyncMode,

  @Convert(converter = SyncInitiateTypeConverter::class)
  @Column(name = "initiate_type", nullable = false, length = 5)
  var initiateType: SyncInitiateType,

  @Column(name = "total_records", nullable = true)
  var totalRecords: Int?,

  @Column(name = "processed_records", nullable = false)
  var processedRecords: Int = 0,

  @Column(name = "skipped_records", nullable = false)
  var skippedRecords: Int = 0,

  @Column(name = "failed_records", nullable = false)
  var failedRecords: Int = 0,

  @Convert(converter = SyncStatusConverter::class)
  @Column(name = "status", nullable = false, length = 20)
  var status: SyncStatus,

  @Column(name = "started_at")
  var startedAt: OffsetDateTime? = null,

  @Column(name = "completed_at")
  var completedAt: OffsetDateTime? = null,

  @Column(name = "canceled_at")
  var canceledAt: OffsetDateTime? = null,

  @Column(name = "last_processed_revision")
  var lastProcessedRevision: Long? = null,

  @Column(name = "error_message", columnDefinition = "TEXT")
  var errorMessage: String? = null

) : HasReferenceEntity() {

  fun getPercentComplete(): Double? {
    return totalRecords?.let { total ->
      if (total > 0) {
        (processedRecords + skippedRecords + failedRecords).toDouble() / total * 100
      } else {
        0.0
      }
    }
  }

}
