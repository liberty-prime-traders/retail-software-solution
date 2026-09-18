package me.ezra_home.retail_software_solution.platform.business.role_assignment

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.PLATFORM_ROLES])
class PlatformRoleCache(private val platformRoleAssignmentRepository: PlatformRoleAssignmentRepository) {

    @Cacheable
    fun getPlatformRoles(userId: UUID): Set<RtsRole> {
        return platformRoleAssignmentRepository.findAllByUserId(userId).map { it.role }.toSet()
    }

    @CacheEvict(allEntries = true)
    fun insert(platformRoleAssignmentEntity: PlatformRoleAssignmentEntity) {
        platformRoleAssignmentRepository.save(platformRoleAssignmentEntity)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, role: RtsRole) {
        platformRoleAssignmentRepository.findByUserIdAndRole(userId, role)
            ?.let { platformRoleAssignmentRepository.delete(it) }
    }
}
