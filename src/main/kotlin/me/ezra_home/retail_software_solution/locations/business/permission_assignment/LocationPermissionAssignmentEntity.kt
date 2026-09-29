package me.ezra_home.retail_software_solution.locations.business.permission_assignment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.model.PermissionAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.LOCATION_PERMISSION_ASSIGNMENT)
class LocationPermissionAssignmentEntity(
    userId: UUID,
    permission: RtsPermission,
    assignedAt: OffsetDateTime,
    assignedById: UUID,

    @Column(name = "location_user_id", nullable = false)
    var locationUserId: UUID

) : PermissionAssignmentEntity(userId, permission, assignedAt, assignedById)
