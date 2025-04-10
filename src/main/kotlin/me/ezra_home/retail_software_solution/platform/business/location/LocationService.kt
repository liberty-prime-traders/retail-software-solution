package me.ezra_home.retail_software_solution.platform.business.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class LocationService(private val locationCache: LocationCache, private val locationMapper: LocationMapper) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getLocationsForOrganization(organizationId: UUID): Collection<LocationResponseDto> {
        return locationCache.getByOrganizationId(organizationId).map {
            locationMapper.toResponseDto(it)
        }
    }

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        validateLocationInsert(locationInsertDto)
        val locationEntity = locationMapper.toEntity(locationInsertDto)
        locationCache.upsertLocation(locationEntity)
        return locationMapper.toResponseDto(locationEntity)
    }

    private fun validateLocationInsert(locationInsertDto: LocationInsertDto) {
        val name = Optional.ofNullable(locationInsertDto.name)
        if (name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        val organizationId = locationInsertDto.organizationId
            ?: throw RtsGenericException(MISSING_ORGANIZATION)
        val siblingLocations = locationCache.getByOrganizationId(organizationId)
        val locationWithMatchingName = siblingLocations.find { it.name.equals(locationInsertDto.name, ignoreCase = true) }
        if (locationWithMatchingName != null) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.get()))
        }
    }

    fun updateLocation(locationUpdateDto: LocationUpdateDto): LocationResponseDto {
        validateLocationUpdate(locationUpdateDto)
        val locationEntity = locationCache.getByOrganizationId(locationUpdateDto.organizationId?.get())
            .find { Objects.equals(it.id, locationUpdateDto.id) }
        if (locationEntity == null) {
            throw UpdatingNonExistingRecordException()
        }
        locationMapper.partialUpdate(locationUpdateDto, locationEntity)
        return locationMapper.toResponseDto(locationEntity)
    }

    private fun validateLocationUpdate(locationUpdateDto: LocationUpdateDto) {
        val name = locationUpdateDto.name
        if (name == null || name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        val organizationId = locationUpdateDto.organizationId
        if (organizationId == null || organizationId.isEmpty) {
            throw RtsGenericException(MISSING_ORGANIZATION)
        }
        val siblingLocations = locationCache.getByOrganizationId(organizationId.get())
        val locationWithMatchingName = siblingLocations.find {
            it.name.equals(name.get(), ignoreCase=true) && !Objects.equals(it.id, locationUpdateDto.id)
        }
        if (locationWithMatchingName != null) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.get()))
        }
    }

    fun deleteLocation(id: UUID?) {
        id?.let {
            locationCache.getAllLocations().find { it.id == id }?.let {entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Location ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                locationCache.deleteLocation(id)
            }
        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A location must have a name"
        const val MISSING_ORGANIZATION = "A location cannot be saved without an organization Id"
        const val NAME_ALREADY_EXISTS = "A location with the name %s is already assigned to the given organization"
    }
}
