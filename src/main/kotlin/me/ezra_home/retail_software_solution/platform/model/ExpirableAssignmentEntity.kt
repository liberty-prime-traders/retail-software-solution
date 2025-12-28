package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class ExpirableAssignmentEntity(
    @Column(name = "user_id", updatable = false)
    var userId: UUID? = null,

    @Column(name = "start_on", updatable = false)
    @CreationTimestamp
    var startOn: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null

): HasReferenceEntity() {
    fun isActive(): Boolean {
        return endOn == null
    }
}
