package me.ezra_home.retail_software_solution.locations.business.role_assignment

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.model.RoleAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.LOCATION_ROLE_ASSIGNMENT)
class LocationRoleAssignmentEntity(
    userId: UUID,
    role: RtsRole,
    assignedAt: OffsetDateTime,
    assignedById: UUID
) : RoleAssignmentEntity(userId, role, assignedAt, assignedById)
