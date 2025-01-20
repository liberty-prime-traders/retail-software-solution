package me.ezra_home.retail_software_solution.business.userlocation

import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.location.LocationCache
import me.ezra_home.retail_software_solution.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.business.userlocation.dto.LocationUserDto
import me.ezra_home.retail_software_solution.business.userlocation.dto.UserLocationRequestDto
import me.ezra_home.retail_software_solution.business.userlocation.dto.UserLocationResponseDto
import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.UserQualifier
import me.ezra_home.retail_software_solution.model.entity.UserLocationEntity
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserLocationService(
    private val userLocationCache: UserLocationCache,
    private val locationCache: LocationCache,
    private val sysUserCache: SysUserCache,
    private val userQualifier: UserQualifier
) {

    @Transactional
    fun getAllUserLocationAssignments(): Collection<UserLocationResponseDto> {
        return userLocationCache.getAll()
            .groupBy { it.locationId }
            .map { UserLocationResponseDto(
                it.key!!,
                createLocationUserDtos(it.value)
            )}
    }

    private fun createLocationUserDtos(userLocations: Collection<UserLocationEntity>): Collection<LocationUserDto> {
        return userLocations.map { userLocationEntity ->
            LocationUserDto(
                fullName = userQualifier.getUserFullName(userLocationEntity.userId),
                userId = userLocationEntity.userId,
                startOn = userLocationEntity.startOn,
                endOn = userLocationEntity.endOn
            )
        }
    }

    @Transactional
    fun updateLocationAssignments(userLocationRequestDto: UserLocationRequestDto): UserLocationResponseDto {
        val locationIdFromRequest = userLocationRequestDto.locationId ?: throw QueriedByEmptyIdException()
        if (locationCache.getDistinctLocationIds().none {locationIdFromRequest == it}) {
            throw NotFoundException()
        }

        removeAssignments(userLocationRequestDto)
        addAssignments(userLocationRequestDto)

        val userLocationsFromDb = userLocationCache.findByLocationId(locationIdFromRequest)
            .groupBy { it.locationId }[locationIdFromRequest]
            .orEmpty()

        return UserLocationResponseDto(
            locationIdFromRequest,
            createLocationUserDtos(userLocationsFromDb)
        )
    }

    private fun removeAssignments(userLocationRequestDto: UserLocationRequestDto) {
        userLocationRequestDto.usersToRemove?.let {
            userLocationCache.terminateLocationAssignments(userLocationRequestDto.locationId!!, it.toSet())
        }
    }

    private fun addAssignments(userLocationRequestDto: UserLocationRequestDto) {
        val locationIdFromRequest = userLocationRequestDto.locationId!!
        userLocationRequestDto.usersToAdd?.let {
            val newUserLocationEntities = it.filter(getUsersEligibleForAssignmentPredicate(locationIdFromRequest))
                .map {
                    UserLocationEntity().apply {
                        userId = it
                        locationId = locationIdFromRequest
                    }
                }
            userLocationCache.saveAll(newUserLocationEntities)
        }
    }

    private fun getUsersEligibleForAssignmentPredicate(locationId: UUID): (UUID) -> Boolean {
        val allActiveAssignments = userLocationCache.getAll().filter { it.endOn == null }
        val allUserIds = sysUserCache.getSystemUsers().map { it.id }
        return { userId -> allUserIds.contains(userId) &&
                allActiveAssignments.none { it.locationId == locationId &&  it.userId == userId }
        }
    }
}
