package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime

@MappedSuperclass
class ExpirableAssignmentEntity (
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
