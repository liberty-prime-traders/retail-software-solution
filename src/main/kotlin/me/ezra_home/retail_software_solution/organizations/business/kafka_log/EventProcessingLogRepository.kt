package me.ezra_home.retail_software_solution.organizations.business.kafka_log

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface EventProcessingLogRepository : JpaRepository<EventProcessingLogEntity, UUID> {

    @Query("SELECT e FROM EventProcessingLogEntity e"
        + " WHERE e.eventId = :eventId AND e.consumerGroup = :consumerGroup "
        + "ORDER BY e.createdOn DESC"
    )
    fun findLatestByEventIdAndConsumerGroup(eventId: UUID, consumerGroup: String): EventProcessingLogEntity?

    fun findBySourceDocumentId(sourceDocumentId: UUID): List<EventProcessingLogEntity>

    fun findByStatusInAndCreatedOnBefore(
        statuses: Collection<EventProcessingLogStatus>,
        before: OffsetDateTime
    ): List<EventProcessingLogEntity>
}
