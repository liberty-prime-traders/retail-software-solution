package me.ezra_home.retail_software_solution.platform.business.role_assignment

import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlatformRoleAssignmentRepository : JpaRepository<PlatformRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<PlatformRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: RtsRole): PlatformRoleAssignmentEntity?
}
