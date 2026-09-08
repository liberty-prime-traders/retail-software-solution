package me.ezra_home.retail_software_solution.organizations.business.kafka_log

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.StringSetConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = TableNames.EVENT_PROCESSING_LOG)
@HasReference(tableName = TableName.EVENT_PROCESSING_LOG)
class EventProcessingLogEntity(

    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: UUID,

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    var eventType: String,

    @Column(name = "consumer_group", updatable = false, length = 50)
    var consumerGroup: String?,

    @Column(name = "source_location_id", updatable = false)
    var sourceLocationId: UUID? = null,

    @Column(name = "source_document_id", nullable = false, updatable = false)
    var sourceDocumentId: UUID,

    @Convert(converter = EventProcessingLogStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: EventProcessingLogStatus,

    @Convert(converter = EventProcessingLogResolutionTypeConverter::class)
    @Column(name = "resolution_type", length = 5)
    var resolutionType: EventProcessingLogResolutionType? = null,

    @Column(name = "processed_on")
    var processedOn: Instant? = null,

    @Column(name = "failed_on")
    var failedOn: Instant? = null,

    @Column(name = "failure_reason", columnDefinition = "text")
    var failureReason: String? = null,

    @Column(name = "dlt_partition")
    var dltPartition: Int? = null,

    @Column(name = "dlt_offset")
    var dltOffset: Long? = null,

    @Convert(converter = StringSetConverter::class)
    @Column(name = "completed_processors", columnDefinition = "text")
    var completedProcessors: Set<String> = emptySet()

) : HasReferenceEntity()
