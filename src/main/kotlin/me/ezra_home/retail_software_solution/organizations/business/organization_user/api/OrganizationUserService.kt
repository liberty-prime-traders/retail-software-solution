package me.ezra_home.retail_software_solution.organizations.business.organization_user.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipPeriod
import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipUserSummary
import me.ezra_home.retail_software_solution.cross_tier.authority.UserAccessDetail
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.api.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.business.organization_user.OrganizationUserCache
import me.ezra_home.retail_software_solution.organizations.business.organization_user.OrganizationUserMapper
import me.ezra_home.retail_software_solution.organizations.business.permission_assignment.api.OrgPermissionAssignmentService
import me.ezra_home.retail_software_solution.organizations.business.role_assignment.api.OrgRoleAssignmentService
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationUserService(
    private val organizationUserCache: OrganizationUserCache,
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserMapper: OrganizationUserMapper,
    private val orgRoleAssignmentService: OrgRoleAssignmentService,
    private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
    private val sysUserService: SysUserService
) {
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun isOrganizationMember(userId: UUID): Boolean {
        return organizationAdminService.isOrganizationAdmin()
                || organizationUserCache.existsByOrganizationIdAndUserId(userId)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getOrganizationUserSummaries(): Collection<MembershipUserSummary> {
        val usersById = sysUserService.getAllUsers().associateBy { it.id }
        return organizationUserCache.getMembershipSummaries()
            .map { row ->
                val user = usersById[row.userId]
                MembershipUserSummary(
                    userId = row.userId,
                    fullName = user?.fullName,
                    email = user?.email,
                    membershipCount = row.membershipCount,
                    active = row.active
                )
            }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getUserAccessDetail(userId: UUID): UserAccessDetail {
        val memberships = organizationUserCache.getOrganizationUsers()
            .filter { it.userId == userId }
            .map { MembershipPeriod(it.id, it.createdOn, it.endOn) }
        return UserAccessDetail(
            userId = userId,
            memberships = memberships,
            roles = orgRoleAssignmentService.getGrantedRoles(userId),
            addOnPermissions = orgPermissionAssignmentService.getGrantedPermissions(userId)
        )
    }

    fun terminateOrganizationUsers(organizationUserIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        if (organizationUserIds.isEmpty()) {
            throw RtsGenericException("No users provided for termination")
        }

        return organizationUserCache.getOrganizationUsers().filter {
            organizationUserIds.contains(it.userId) && it.isActive()
        }.map { dto ->
            val saved = organizationUserCache.save(dto.copy(endOn = OffsetDateTime.now()))
            orgRoleAssignmentService.removeAllRoles(dto.userId)
            orgPermissionAssignmentService.removeAllPermissions(dto.userId)
            organizationUserMapper.toDto(saved)
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getActiveMembershipIds(userIds: Collection<UUID>): Map<UUID, UUID> {
        val activeMembershipIdByUserId = organizationUserCache.getOrganizationUsers()
            .filter { it.isActive() }
            .associate { it.userId to it.id }
        return userIds.associateWith { userId ->
            activeMembershipIdByUserId[userId]
                ?: throw RtsGenericException("User is not an active member of this organization")
        }
    }

    fun admitJoinRequests(joinRequests: Collection<OrganizationAdminJoinRequestResponseDto>) {
        joinRequests.map { OrganizationUserInsertDto(joinRequestId = it.id, userId = it.createdById) }
            .let { organizationUserCache.createAll(it) }
    }

    fun registerFounder(userId: UUID) {
        val membership = organizationUserCache.create(OrganizationUserInsertDto(userId = userId))
        val assignedById = SessionContextProvider.getUserId()
        val membershipIdByUserId = mapOf(userId to membership.id)
        orgRoleAssignmentService.assignAll(membershipIdByUserId, RtsRole.MANAGE_ORGANIZATION_USERS, assignedById)
    }
}
