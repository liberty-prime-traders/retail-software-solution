package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsPermissionConverter
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class PermissionAssignmentEntity(

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "permission", nullable = false, length = 5)
    @Convert(converter = RtsPermissionConverter::class)
    var permission: RtsPermission,

    @Column(name = "assigned_at", nullable = false)
    var assignedAt: OffsetDateTime,

    @Column(name = "assigned_by_id", nullable = false)
    var assignedById: UUID

) : BaseEntity()
