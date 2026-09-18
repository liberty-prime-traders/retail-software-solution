package me.ezra_home.retail_software_solution.organizations.business.role_assignment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.model.RoleAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.ORG_ROLE_ASSIGNMENT)
class OrgRoleAssignmentEntity(
    userId: UUID,
    role: RtsRole,
    assignedAt: OffsetDateTime,
    assignedById: UUID,

    @Column(name = "org_user_id", nullable = false)
    var orgUserId: UUID

) : RoleAssignmentEntity(userId, role, assignedAt, assignedById)
