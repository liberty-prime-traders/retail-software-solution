package me.ezra_home.retail_software_solution.organizations.business.role_assignment

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.cross_tier.authority.GrantedRole
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_ROLES])
class OrgRoleCache(private val orgRoleAssignmentRepository: OrgRoleAssignmentRepository) {

    @Cacheable
    fun getOrgRoles(userId: UUID): Set<RtsRole> {
        return orgRoleAssignmentRepository.findAllByUserId(userId).map { it.role }.toSet()
    }

    @Cacheable
    fun getGrantedRoles(userId: UUID): List<GrantedRole> {
        return orgRoleAssignmentRepository.findAllByUserId(userId).map { GrantedRole(it.role, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(orgRoleAssignmentEntities: Collection<OrgRoleAssignmentEntity>) {
        orgRoleAssignmentRepository.saveAll(orgRoleAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, role: RtsRole) {
        orgRoleAssignmentRepository.findByUserIdAndRole(userId, role)
            ?.let { orgRoleAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingRole(userIds: Collection<UUID>, role: RtsRole) {
        orgRoleAssignmentRepository.deleteAll(orgRoleAssignmentRepository.findAllByUserIdInAndRole(userIds, role))
    }

    @CacheEvict(allEntries = true)
    fun removeAll(userId: UUID) {
        orgRoleAssignmentRepository.deleteAll(orgRoleAssignmentRepository.findAllByUserId(userId))
    }
}
