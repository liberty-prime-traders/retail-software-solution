package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.locationadmin.LocationAdminCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationUsageCounter
import me.ezra_home.retail_software_solution.organizations.model.LocationAdminEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects

@Service
@TransactionalOnOrganizationSchema
class LocationService(
    private val locationCache: LocationCache,
    private val locationMapper: LocationMapper,
    private val locationValidator: LocationValidator,
    private val locationAdminCache: LocationAdminCache,
    private val organizationUsageCounter: OrganizationUsageCounter,
    private val locationSchemaCreator: LocationSchemaCreator
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getLocationsForOrganization(): Collection<LocationResponseDto> {
        return locationCache.getByOrganizationId(SessionContextProvider.getOrganizationId()).map {
            locationMapper.toResponseDto(it)
        }
    }

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        locationValidator.validateLocationInsert(locationInsertDto, organizationId)
        val schemaName = createLocationSchema(locationInsertDto.name!!)
        val locationEntity = locationMapper.toEntity(locationInsertDto).apply {
            this.organizationId = organizationId
            this.schemaName = schemaName
        }
        locationCache.upsertLocation(locationEntity)
        locationAdminCache.upsertLocationAdmin(LocationAdminEntity(locationEntity.id).apply { adminId = locationEntity.createdById })
        organizationUsageCounter.incrementUsageCount(organizationId)
        return locationMapper.toResponseDto(locationEntity)
    }

    private fun createLocationSchema(locationName: String): String {
        val schemaName = "loc_${locationName.lowercase().replace(" ", "_")}"
        locationSchemaCreator.createSchema(schemaName)
        return schemaName
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
