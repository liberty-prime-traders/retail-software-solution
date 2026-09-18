package me.ezra_home.retail_software_solution.locations.business.location_user.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_user.LocationUserCache
import me.ezra_home.retail_software_solution.locations.business.location_user.LocationUserMapper
import me.ezra_home.retail_software_solution.locations.business.role_assignment.api.LocationRoleAssignmentService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationUserService(
    private val locationUserCache: LocationUserCache,
    private val locationUserMapper: LocationUserMapper,
    private val locationRoleAssignmentService: LocationRoleAssignmentService
) {
    @TransactionalOnLocationSchema(readOnly = true)
    fun getLocationUsers(): Collection<LocationUserResponseDto> {
        return locationUserCache.getLocationUsers()
            .map { locationUserMapper.toDto(it) }
    }

    fun createLocationUser(locationUserInsertDto: LocationUserInsertDto): LocationUserResponseDto {
        val saved = locationUserCache.create(locationUserInsertDto)
        return locationUserMapper.toDto(saved)
    }

    fun terminateLocationUsers(locationUserIds: List<UUID>): Collection<LocationUserResponseDto> {
        if (locationUserIds.isEmpty()) {
            throw RtsGenericException("No users provided for termination")
        }

        return locationUserCache.getLocationUsers().filter {
            locationUserIds.contains(it.id) && it.isActive()
        }.map { dto ->
            val saved = locationUserCache.save(dto.copy(endOn = OffsetDateTime.now()))
            locationRoleAssignmentService.removeAllRoles(dto.userId)
            locationUserMapper.toDto(saved)
        }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getActiveMembershipId(userId: UUID): UUID {
        return locationUserCache.getLocationUsers()
            .firstOrNull { it.userId == userId && it.isActive() }
            ?.id
            ?: throw RtsGenericException("User is not an active member of this location")
    }
}
