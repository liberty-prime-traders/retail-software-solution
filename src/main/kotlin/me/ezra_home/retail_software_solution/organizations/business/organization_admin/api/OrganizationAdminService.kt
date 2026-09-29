package me.ezra_home.retail_software_solution.organizations.business.organization_admin.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminMapper
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationAdminService(
    private val organizationAdminMapper: OrganizationAdminMapper,
    private val organizationAdminCache: OrganizationAdminCache,
    private val sysUserService: SysUserService,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAdminHistory(): Collection<OrganizationAdminResponseDto> {
        return organizationAdminCache.getAdminHistory()
            .map { organizationAdminMapper.toResponseDto(it) }
    }

    fun createOrganizationAdmin(adminId: UUID): OrganizationAdminResponseDto {
        validateUserExists(adminId)
        val dto = organizationAdminCache.create(OrganizationAdminInsertDto(userId = adminId))
        return organizationAdminMapper.toResponseDto(dto)
    }

    private fun validateUserExists(adminId: UUID) {
        sysUserService.getAllUsers().find { it.id == adminId }
            ?: throw RtsGenericException("User with id $adminId not found")
    }

    fun terminateOrganizationAdmin(adminId: UUID) {
        organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == adminId }
            ?.let { organizationAdminCache.save(it.copy(endOn = OffsetDateTime.now())) }
    }

    fun isOrganizationAdmin(): Boolean {
        val userId = SessionContextProvider.getUserId()
        return organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == userId } != null
    }

    fun registerFounder(userId: UUID) {
        organizationAdminCache.create(OrganizationAdminInsertDto(userId = userId))
    }
}
