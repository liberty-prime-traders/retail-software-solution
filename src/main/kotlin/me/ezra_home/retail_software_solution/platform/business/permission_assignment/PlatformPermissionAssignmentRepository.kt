package me.ezra_home.retail_software_solution.platform.business.permission_assignment

import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlatformPermissionAssignmentRepository : JpaRepository<PlatformPermissionAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<PlatformPermissionAssignmentEntity>
    fun findByUserIdAndPermission(userId: UUID, permission: RtsPermission): PlatformPermissionAssignmentEntity?
    fun findAllByPermission(permission: RtsPermission): List<PlatformPermissionAssignmentEntity>
    fun findAllByUserIdInAndPermission(userIds: Collection<UUID>, permission: RtsPermission): List<PlatformPermissionAssignmentEntity>
}
