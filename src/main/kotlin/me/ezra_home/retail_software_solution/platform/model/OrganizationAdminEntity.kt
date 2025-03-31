package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_ADMIN)
class OrganizationAdminEntity(

    @Column(name = "organization_id", updatable = false)
    var organizationId: UUID? = null,

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
