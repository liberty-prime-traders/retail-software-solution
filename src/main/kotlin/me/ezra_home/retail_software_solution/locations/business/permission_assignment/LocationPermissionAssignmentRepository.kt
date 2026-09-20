package me.ezra_home.retail_software_solution.locations.business.permission_assignment

import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationPermissionAssignmentRepository : JpaRepository<LocationPermissionAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<LocationPermissionAssignmentEntity>
    fun findByUserIdAndPermission(userId: UUID, permission: RtsPermission): LocationPermissionAssignmentEntity?
    fun findAllByUserIdInAndPermission(userIds: Collection<UUID>, permission: RtsPermission): List<LocationPermissionAssignmentEntity>
}
