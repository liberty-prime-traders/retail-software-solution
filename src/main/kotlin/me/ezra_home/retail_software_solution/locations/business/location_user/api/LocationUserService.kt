package me.ezra_home.retail_software_solution.locations.business.location_user.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipPeriod
import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipSummaryRow
import me.ezra_home.retail_software_solution.cross_tier.authority.MembershipUserSummary
import me.ezra_home.retail_software_solution.cross_tier.authority.UserAccessDetail
import me.ezra_home.retail_software_solution.locations.business.location_user.LocationUserCache
import me.ezra_home.retail_software_solution.locations.business.location_user.LocationUserMapper
import me.ezra_home.retail_software_solution.locations.business.permission_assignment.api.LocationPermissionAssignmentService
import me.ezra_home.retail_software_solution.locations.business.role_assignment.api.LocationRoleAssignmentService
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserWithProfileDto
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationUserService(
    private val locationUserCache: LocationUserCache,
    private val locationUserMapper: LocationUserMapper,
    private val locationRoleAssignmentService: LocationRoleAssignmentService,
    private val locationPermissionAssignmentService: LocationPermissionAssignmentService,
    private val sysUserService: SysUserService
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getLocationUserSummaries(): Collection<MembershipUserSummary> {
        val usersById = sysUserService.getAllUsers().associateBy { it.id }
        return locationUserCache.getMembershipSummaries()
            .map { row -> toMembershipUserSummary(row, usersById[row.userId]) }
    }

    private fun toMembershipUserSummary(
        membershipSummaryRow: MembershipSummaryRow,
        sysUserWithProfileDto: SysUserWithProfileDto?
    ) = MembershipUserSummary(
        userId = membershipSummaryRow.userId,
        fullName = sysUserWithProfileDto?.fullName,
        email = sysUserWithProfileDto?.email,
        membershipCount = membershipSummaryRow.membershipCount,
        active = membershipSummaryRow.active
    )

    @TransactionalOnLocationSchema(readOnly = true)
    fun getUserAccessDetail(userId: UUID): UserAccessDetail {
        val memberships = locationUserCache.getLocationUsers()
            .filter { it.userId == userId }
            .map { MembershipPeriod(it.id, it.createdOn, it.endOn) }
        return UserAccessDetail(
            userId = userId,
            memberships = memberships,
            roles = locationRoleAssignmentService.getGrantedRoles(userId),
            addOnPermissions = locationPermissionAssignmentService.getGrantedPermissions(userId)
        )
    }

    fun createLocationUsers(userIds: List<UUID>): Collection<MembershipUserSummary> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val saved = locationUserCache.createAll(userIds.map { LocationUserInsertDto(userId = it) })
        val usersById = sysUserService.getAllUsers().associateBy { it.id }
        val membershipSummaryRowsByUserId = locationUserCache.getMembershipSummaries().associateBy { it.userId }
        return saved.map { locationUserDto ->
            toMembershipUserSummary(membershipSummaryRowsByUserId.getValue(locationUserDto.userId), usersById[locationUserDto.userId])
        }
    }

    fun registerFounder(userId: UUID) {
        val membership = locationUserCache.create(LocationUserInsertDto(userId = userId))
        val assignedById = SessionContextProvider.getUserId()
        locationPermissionAssignmentService.assignAll(
            mapOf(userId to membership.id),
            RtsPermission.MANAGE_LOCATION_ACCESS,
            assignedById
        )
    }

    fun terminateLocationUsers(locationUserIds: List<UUID>): Collection<LocationUserResponseDto> {
        if (locationUserIds.isEmpty()) {
            throw RtsGenericException("No users provided for termination")
        }

        return locationUserCache.getLocationUsers().filter {
            locationUserIds.contains(it.userId) && it.isActive()
        }.map { dto ->
            val saved = locationUserCache.save(dto.copy(endOn = OffsetDateTime.now()))
            locationRoleAssignmentService.removeAllRoles(dto.userId)
            locationPermissionAssignmentService.removeAllPermissions(dto.userId)
            locationUserMapper.toDto(saved)
        }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getActiveMembershipId(userId: UUID): UUID = getActiveMembershipIds(listOf(userId)).getValue(userId)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getActiveMembershipIds(userIds: Collection<UUID>): Map<UUID, UUID> {
        val activeMembershipIdByUserId = locationUserCache.getLocationUsers()
            .filter { it.isActive() }
            .associate { it.userId to it.id }
        return userIds.associateWith { userId ->
            activeMembershipIdByUserId[userId]
                ?: throw RtsGenericException("User is not an active member of this location")
        }
    }
}
