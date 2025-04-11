package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
class BaseAdminEntity(
    @Column(name = "admin_id", updatable = false)
    var adminId: UUID? = null,

    @Column(name = "start_on", updatable = false)
    var startOn: OffsetDateTime? = null,

    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null

): BaseEntity() {
    fun isActive(): Boolean {
        return endOn == null
    }
}
