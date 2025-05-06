package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
import me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationAdminService(
    private val organizationAdminMapper: OrganizationAdminMapper,
    private val organizationAdminCache: OrganizationAdminCache,
    private val sysUserService: SysUserService,
    private val organizationCache: OrganizationCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAdminHistoryForOrganization(): Collection<OrganizationAdminResponseDto> {
        val organizationId = SessionContextProvider.getOrganizationId()
        return organizationAdminCache.getAdminHistoryForOrganization(organizationId)
            .map { organizationAdminMapper.toResponseDto(it) }
    }

    fun createOrganizationAdmin(adminId: UUID): OrganizationAdminResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        validateUserAndOrganizationExist(adminId, organizationId)
        val entity = OrganizationAdminEntity(organizationId).apply { this.adminId = adminId }
        organizationAdminCache.upsertOrganizationAdmin(entity)
        return organizationAdminMapper.toResponseDto(entity)
    }

    private fun validateUserAndOrganizationExist(adminId: UUID, organizationId: UUID?) {
        organizationCache.getAllOrganizations().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization with id $organizationId not found")
        sysUserService.getAllUsers().find { it.id == adminId }
            ?: throw RtsGenericException("User with id $adminId not found")
    }

    fun terminateOrganizationAdmin(adminId: UUID) {
        val organizationId = SessionContextProvider.getOrganizationId()
        organizationAdminCache.getAdminHistoryForOrganization(organizationId)
            .find { it.isActive() && it.adminId == adminId }
            ?.let {
                it.endOn = OffsetDateTime.now()
                organizationAdminCache.upsertOrganizationAdmin(it)
            }
    }
}
