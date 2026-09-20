package me.ezra_home.retail_software_solution.organizations.business.role_assignment

import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrgRoleAssignmentRepository : JpaRepository<OrgRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<OrgRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: RtsRole): OrgRoleAssignmentEntity?
    fun findAllByUserIdInAndRole(userIds: Collection<UUID>, role: RtsRole): List<OrgRoleAssignmentEntity>
}
