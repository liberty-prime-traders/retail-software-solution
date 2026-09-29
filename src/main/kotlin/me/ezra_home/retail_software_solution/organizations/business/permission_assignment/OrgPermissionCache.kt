package me.ezra_home.retail_software_solution.organizations.business.permission_assignment

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.cross_tier.authority.GrantedPermission
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_PERMISSIONS])
class OrgPermissionCache(private val orgPermissionAssignmentRepository: OrgPermissionAssignmentRepository) {

    @Cacheable
    fun getOrgPermissions(userId: UUID): Set<RtsPermission> {
        return orgPermissionAssignmentRepository.findAllByUserId(userId).map { it.permission }.toSet()
    }

    @Cacheable
    fun getGrantedPermissions(userId: UUID): List<GrantedPermission> {
        return orgPermissionAssignmentRepository.findAllByUserId(userId).map { GrantedPermission(it.permission, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(orgPermissionAssignmentEntities: Collection<OrgPermissionAssignmentEntity>) {
        orgPermissionAssignmentRepository.saveAll(orgPermissionAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, permission: RtsPermission) {
        orgPermissionAssignmentRepository.findByUserIdAndPermission(userId, permission)
            ?.let { orgPermissionAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingPermission(userIds: Collection<UUID>, permission: RtsPermission) {
        orgPermissionAssignmentRepository.deleteAll(
            orgPermissionAssignmentRepository.findAllByUserIdInAndPermission(userIds, permission)
        )
    }

    @CacheEvict(allEntries = true)
    fun removeAll(userId: UUID) {
        orgPermissionAssignmentRepository.deleteAll(orgPermissionAssignmentRepository.findAllByUserId(userId))
    }
}
