package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationUserService(
    private val organizationUserCache: OrganizationUserCache,
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserMapper: OrganizationUserMapper
) {
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun isOrganizationMember(userId: UUID): Boolean {
        return if (organizationAdminService.isOrganizationAdmin()) {
            true
        } else {
            organizationUserCache.existsByOrganizationIdAndUserId(userId)
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getOrganizationUsers(): Collection<OrganizationUserResponseDto> {
        return organizationUserCache.getOrganizationUsers()
            .map { organizationUserMapper.toDto(it) }
    }

    fun terminateOrganizationUsers(organizationUserIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        if (organizationUserIds.isEmpty()) {
            throw RtsGenericException("No users provided for termination")
        }

        return organizationUserCache.getOrganizationUsers().filter {
            organizationUserIds.contains(it.id) && it.isActive()
        }.map { organizationUserEntity ->
            organizationUserEntity.endOn = OffsetDateTime.now()
            organizationUserCache.upsertOrganizationUser(organizationUserEntity)
            organizationUserMapper.toDto(organizationUserEntity)
        }
    }

    fun admitJoinRequests(joinRequests: Collection<OrganizationAdminJoinRequestResponseDto>) {
        joinRequests.map { OrganizationUserEntity(it.id).apply { userId = it.createdById } }
            .let { organizationUserCache.upsertOrganizationUsers(it)}
    }
}
