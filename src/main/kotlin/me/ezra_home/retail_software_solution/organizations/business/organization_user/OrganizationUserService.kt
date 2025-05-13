package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationUserService(
    private val organizationUserCache: OrganizationUserCache,
) {
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun isOrganizationMember(userId: UUID): Boolean {
        return organizationUserCache.existsByOrganizationIdAndUserId(userId)
    }

    fun createOrganizationUser(organizationUserEntity: OrganizationUserEntity) {
        organizationUserCache.upsertOrganizationUser(organizationUserEntity)
    }
}
