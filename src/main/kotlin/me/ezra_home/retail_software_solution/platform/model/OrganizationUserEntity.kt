package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_USER)
class OrganizationUserEntity(

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,

    @Column(name = "organization_id", nullable = false, updatable = false)
    var organizationId: UUID,

    @Column(name = "start_on", nullable = false)
    var startOn: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null,

    @Column(name = "join_request_id", updatable = false)
    var joinRequestId: UUID? = null

) : BaseEntity()
