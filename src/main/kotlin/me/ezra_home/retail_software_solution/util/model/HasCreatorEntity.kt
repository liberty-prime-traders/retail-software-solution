package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class HasCreatorEntity(
    @CreatedBy
    @Column(name = "created_by_id", updatable = false)
    var createdById: UUID? = null,

    @Column(name = "created_on", updatable = false)
    var createdOn: OffsetDateTime? = null

): BaseEntity() {

    // Stamped here so it is readable straight after save(), without a flush. Hibernate's
    // @CreationTimestamp applies at INSERT, which is too late for callers that map the saved
    // entity onto a DTO; Spring's @CreatedDate cannot convert to OffsetDateTime.
    //
    // System zone, not organization: this runs for platform-level inserts and for Kafka consumers
    // with no organization in session, and the column is timestamptz so only the instant persists.
    override fun prePersist() {
        super.prePersist()
        if (createdOn == null) createdOn = DateTimes.Offset.Now.system()
    }

    fun requiredCreatedOn(): OffsetDateTime = createdOn
        ?: throw RtsGenericException("${javaClass.simpleName} is missing its createdOn timestamp")
}
