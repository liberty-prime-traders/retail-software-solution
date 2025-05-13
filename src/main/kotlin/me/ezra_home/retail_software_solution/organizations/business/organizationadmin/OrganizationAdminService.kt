package me.ezra_home.retail_software_solution.organizations.business.organizationadmin

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationAdminService(
    private val organizationAdminMapper: OrganizationAdminMapper,
    private val organizationAdminCache: OrganizationAdminCache,
    private val sysUserService: SysUserService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAdminHistory(): Collection<OrganizationAdminResponseDto> {
        return organizationAdminCache.getAdminHistory()
            .map { organizationAdminMapper.toResponseDto(it) }
    }

    fun createOrganizationAdmin(adminId: UUID): OrganizationAdminResponseDto {
        validateUserExists(adminId)
        val entity = OrganizationAdminEntity(adminId)
        organizationAdminCache.upsertOrganizationAdmin(entity)
        return organizationAdminMapper.toResponseDto(entity)
    }

    private fun validateUserExists(adminId: UUID) {
        sysUserService.getAllUsers().find { it.id == adminId }
            ?: throw RtsGenericException("User with id $adminId not found")
    }

    fun terminateOrganizationAdmin(adminId: UUID) {
        organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == adminId }
            ?.let {
                it.endOn = OffsetDateTime.now()
                organizationAdminCache.upsertOrganizationAdmin(it)
            }
    }

    fun isOrganizationAdmin(userId: UUID): Boolean {
        return organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == userId } != null
    }
}
