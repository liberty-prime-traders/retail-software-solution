package me.ezra_home.retail_software_solution.platform.business.permission_assignment

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityGrant
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.PLATFORM_PERMISSIONS])
class PlatformPermissionCache(private val platformPermissionAssignmentRepository: PlatformPermissionAssignmentRepository) {

    @Cacheable
    fun getPlatformPermissions(userId: UUID): Set<RtsPermission> {
        return platformPermissionAssignmentRepository.findAllByUserId(userId).map { it.permission }.toSet()
    }

    @Cacheable
    fun getHolders(permission: RtsPermission): List<AuthorityGrant> {
        return platformPermissionAssignmentRepository.findAllByPermission(permission).map { AuthorityGrant(it.userId, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(platformPermissionAssignmentEntities: Collection<PlatformPermissionAssignmentEntity>) {
        platformPermissionAssignmentRepository.saveAll(platformPermissionAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, permission: RtsPermission) {
        platformPermissionAssignmentRepository.findByUserIdAndPermission(userId, permission)
            ?.let { platformPermissionAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingPermission(userIds: Collection<UUID>, permission: RtsPermission) {
        platformPermissionAssignmentRepository.deleteAll(
            platformPermissionAssignmentRepository.findAllByUserIdInAndPermission(userIds, permission)
        )
    }
}
