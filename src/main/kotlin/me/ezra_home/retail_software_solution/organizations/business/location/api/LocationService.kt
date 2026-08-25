package me.ezra_home.retail_software_solution.organizations.business.location.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.business.location.LocationMapper
import me.ezra_home.retail_software_solution.organizations.business.location.LocationSchemaService
import me.ezra_home.retail_software_solution.organizations.business.location.LocationValidator
import me.ezra_home.retail_software_solution.util.business.SchemaNameGenerator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class LocationService(
    private val locationCache: LocationCache,
    private val locationMapper: LocationMapper,
    private val locationValidator: LocationValidator,
    private val locationSchemaService: LocationSchemaService,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllLocations(): Collection<LocationResponseDto> {
        return locationCache.getAllLocations().map { locationMapper.toResponseDto(it) }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllLocationDtos(): Collection<LocationDto> = locationCache.getAllLocations()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getBySchema(schema: String): LocationDto =
        locationCache.getAllLocations().find { it.schemaName == schema }
            ?: throw RtsGenericException("No location found for schema $schema.")

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getById(locationId: UUID): LocationDto =
        locationCache.getAllLocations().find { it.id == locationId }
            ?: throw RtsGenericException("No location found for $locationId.")

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getSchemaByLocationId(locationId: UUID): String =
        locationCache.getAllLocations().find { it.id == locationId }?.schemaName
            ?: throw RtsGenericException("Location schema not found for $locationId")

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        locationValidator.validateLocationInsert(locationInsertDto)
        val schemaName = createLocationSchema(locationInsertDto.name!!)
        try {
            val locationDto = locationCache.create(locationInsertDto, schemaName)
            return locationMapper.toResponseDto(locationDto)
        } catch (e: Exception) {
            locationSchemaService.dropSchema(schemaName)
            throw e
        }
    }

    private fun createLocationSchema(locationName: String): String {
        val schemaName = SchemaNameGenerator.generateSchemaName(locationName, "loc")
        locationSchemaService.createSchema(schemaName)
        return schemaName
    }

    fun updateLocation(locationUpdateDto: LocationUpdateDto): LocationResponseDto {
        val locationId = SessionContextProvider.getLocationId()
        locationValidator.validateLocationUpdate(locationUpdateDto)
        val existing = locationCache.getAllLocations().find { Objects.equals(it.id, locationId) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = locationUpdateDto.applyTo(existing)
        val saved = locationCache.save(updated)
        return locationMapper.toResponseDto(saved)
    }
}
