package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.OffsetDateTime

@MappedSuperclass
internal class ExpirableAssignmentEntity(
    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null

): HasReferenceEntity() {
    fun isActive(): Boolean {
        return endOn == null
    }
}
