package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminInsertDto
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
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
    private val organizationCache: OrganizationCache,
    private val organizationAdminValidator: OrganizationAdminValidator
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAdminHistoryForOrganization(organizationId: UUID?): Collection<OrganizationAdminResponseDto> {
        organizationAdminValidator.canCurrentUserOperateOnOrganization(organizationId)
        return organizationAdminCache.getAdminHistoryForOrganization(organizationId)
            .map { organizationAdminMapper.toResponseDto(it) }
    }

    fun createOrganizationAdmin(organizationInsertDto: OrganizationAdminInsertDto): OrganizationAdminResponseDto {
        validateUserAndOrganizationExist(organizationInsertDto)
        organizationAdminValidator.canCurrentUserOperateOnOrganization(organizationInsertDto.organizationId)
        val entity = organizationAdminMapper.toEntity(organizationInsertDto)
        organizationAdminCache.upsertOrganization(entity)
        return organizationAdminMapper.toResponseDto(entity)
    }

    private fun validateUserAndOrganizationExist(organizationInsertDto: OrganizationAdminInsertDto) {
        organizationCache.getAllOrganizations().find { it.id == organizationInsertDto.organizationId }
            ?: throw RtsGenericException("Organization with id ${organizationInsertDto.organizationId} not found")
        sysUserService.getAllUsers().find { it.id == organizationInsertDto.adminId }
            ?: throw RtsGenericException("User with id ${organizationInsertDto.adminId} not found")
    }

    fun terminateOrganizationAdmin(organizationInsertDto: OrganizationAdminInsertDto) {
        organizationAdminValidator.canCurrentUserOperateOnOrganization(organizationInsertDto.organizationId)
        organizationAdminCache.getAdminHistoryForOrganization(organizationInsertDto.organizationId)
            .find { it.endOn == null && it.adminId == organizationInsertDto.adminId }
            ?.let {
                it.endOn = OffsetDateTime.now()
                organizationAdminCache.upsertOrganization(it)
            }
    }
}
