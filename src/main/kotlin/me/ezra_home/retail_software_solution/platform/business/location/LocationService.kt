package me.ezra_home.retail_software_solution.platform.business.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationUsageCounter
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class LocationService(
    private val locationCache: LocationCache,
    private val locationMapper: LocationMapper,
    private val locationValidator: LocationValidator,
    private val organizationUsageCounter: OrganizationUsageCounter
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getLocationsForOrganization(organizationId: UUID): Collection<LocationResponseDto> {
        return locationCache.getByOrganizationId(organizationId).map {
            locationMapper.toResponseDto(it)
        }
    }

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        locationValidator.validateLocationInsert(locationInsertDto, organizationId)
        val locationEntity = locationMapper.toEntity(locationInsertDto).apply { this.organizationId = organizationId }
        locationCache.upsertLocation(locationEntity)
        organizationUsageCounter.incrementUsageCount(organizationId)
        return locationMapper.toResponseDto(locationEntity)
    }

    fun updateLocation(locationUpdateDto: LocationUpdateDto): LocationResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        val locationId = SessionContextProvider.getLocationId()
        locationValidator.validateLocationUpdate(locationUpdateDto, organizationId)
        val locationEntity = locationCache.getByOrganizationId(organizationId).find { Objects.equals(it.id, locationId) }
        if (locationEntity == null) {
            throw UpdatingNonExistingRecordException()
        }
        locationMapper.partialUpdate(locationUpdateDto, locationEntity)
        return locationMapper.toResponseDto(locationEntity)
    }

    fun deleteLocation() {
        val locationId = SessionContextProvider.getLocationId()
        locationCache.getAllLocations().find { it.id == locationId }?.let {entity ->
            val usageCount = entity.usageCount
            if (usageCount > 0L) {
                throw RtsGenericException("Location ${entity.name} has $usageCount usage(s) and cannot be deleted")
            }
            locationCache.deleteLocation(locationId)
            organizationUsageCounter.decrementUsageCount(SessionContextProvider.getOrganizationId())
        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A location must have a name"
        const val NAME_ALREADY_EXISTS = "A location with the name %s is already assigned to the given organization"
    }
}
