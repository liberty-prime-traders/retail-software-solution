package me.ezra_home.retail_software_solution.platform.business.organization_user

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.model.OrganizationUserEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationUserService(
    private val organizationUserCache: OrganizationUserCache,
) {
    fun existsByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): Boolean {
        return organizationUserCache.existsByOrganizationIdAndUserId(organizationId, userId)
    }

    fun createOrganizationUser(organizationUserEntity: OrganizationUserEntity) {
        organizationUserCache.upsertOrganizationUser(organizationUserEntity)
    }
}