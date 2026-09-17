package me.ezra_home.retail_software_solution.locations.business.role_assignment

import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationRoleAssignmentRepository : JpaRepository<LocationRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<LocationRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: RtsRole): LocationRoleAssignmentEntity?
}
