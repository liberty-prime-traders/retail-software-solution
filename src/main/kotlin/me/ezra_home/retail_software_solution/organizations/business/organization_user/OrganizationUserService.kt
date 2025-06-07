package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.business.organization_user.dto.OrganizationUserResponseDto
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestCache
import me.ezra_home.retail_software_solution.platform.model.OrganizationJoinRequestEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationUserService(
    private val organizationUserCache: OrganizationUserCache,
    private val organizationJoinRequestCache: OrganizationJoinRequestCache,
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

    fun admitUsers(joinRequestIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        if (joinRequestIds.isEmpty()) {
            throw RtsGenericException("No users provided for admission")
        }

        joinRequestIds.map { requestId ->
            val joinRequest =
                organizationJoinRequestCache.getOrganizationJoinRequests(SessionContextProvider.getOrganizationId())
                    .find { it.id == requestId }

            if (joinRequest == null) {
                throw RtsGenericException("Join request with ID '$requestId' could not found")
            }

            validateUserJoinRequest(joinRequest)

            joinRequest.status = JoinRequestStatus.APPROVED
            organizationJoinRequestCache.upsertOrganizationJoinRequest(joinRequest)

            val organizationUser = organizationUserCache.getOrganizationUsers().find { it.joinRequestId == requestId }

            val organizationUserEntity = organizationUser?.apply { startOn = OffsetDateTime.now() }
                ?: OrganizationUserEntity(
                    joinRequestId = requestId,
                ).apply {
                    userId =
                        joinRequest.createdById ?: throw RtsGenericException("No user associated with join request")
                    startOn = OffsetDateTime.now()
                }

            organizationUserCache.upsertOrganizationUser(organizationUserEntity)
            organizationUserEntity
        }

        return getOrganizationUsers()
    }

    private fun validateUserJoinRequest(
        request: OrganizationJoinRequestEntity,
    ) {
        when (request.status) {
            JoinRequestStatus.APPROVED -> throw RtsGenericException("User join request was already approved")
            JoinRequestStatus.DENIED -> throw RtsGenericException("User join request was denied")
            else -> {}
        }
    }

    fun terminateOrganizationUsers(organizationUserIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        if (organizationUserIds.isEmpty()) {
            throw RtsGenericException("No users provided for termination")
        }

        organizationUserIds.forEach { organizationUserId ->
            val organizationUserEntity = organizationUserCache.getOrganizationUsers()
                .find { it.id == organizationUserId }

            if (organizationUserEntity == null) {
                throw RtsGenericException("Organization user with ID '$organizationUserId' does not exist")
            }

            if (organizationUserEntity.isActive().not()) {
                throw RtsGenericException("Organization user '${organizationUserMapper.toDto(organizationUserEntity).user}' is already terminated")
            }

            organizationUserEntity.endOn = OffsetDateTime.now()

            organizationUserCache.upsertOrganizationUser(organizationUserEntity)
        }
        return getOrganizationUsers()
    }
}
