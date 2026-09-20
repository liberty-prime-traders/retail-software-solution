package me.ezra_home.retail_software_solution.organizations.business.permission_assignment

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
@Table(name = TableNames.ORG_PERMISSION_ASSIGNMENT)
class OrgPermissionAssignmentEntity(
    userId: UUID,
    permission: RtsPermission,
    assignedAt: OffsetDateTime,
    assignedById: UUID,

    @Column(name = "org_user_id", nullable = false)
    var orgUserId: UUID

) : PermissionAssignmentEntity(userId, permission, assignedAt, assignedById)
