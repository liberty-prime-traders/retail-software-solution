package me.ezra_home.retail_software_solution.platform.business.permission_assignment

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.model.PermissionAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.PLATFORM_PERMISSION_ASSIGNMENT)
class PlatformPermissionAssignmentEntity(
    userId: UUID,
    permission: RtsPermission,
    assignedAt: OffsetDateTime,
    assignedById: UUID
) : PermissionAssignmentEntity(userId, permission, assignedAt, assignedById)
