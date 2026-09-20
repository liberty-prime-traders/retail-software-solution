package me.ezra_home.retail_software_solution.organizations.business.permission_assignment

import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrgPermissionAssignmentRepository : JpaRepository<OrgPermissionAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<OrgPermissionAssignmentEntity>
    fun findByUserIdAndPermission(userId: UUID, permission: RtsPermission): OrgPermissionAssignmentEntity?
    fun findAllByUserIdInAndPermission(userIds: Collection<UUID>, permission: RtsPermission): List<OrgPermissionAssignmentEntity>
}
