package me.ezra_home.retail_software_solution.organizations.business.location.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.business.location.LocationMapper
import me.ezra_home.retail_software_solution.organizations.business.location.LocationSchemaService
import me.ezra_home.retail_software_solution.organizations.business.location.LocationValidator
import me.ezra_home.retail_software_solution.util.business.SchemaNameGenerator
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects

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

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        locationValidator.validateLocationInsert(locationInsertDto)
        val schemaName = createLocationSchema(locationInsertDto.name!!)
        try {
            val locationDto = locationMapper.toDomainDto(locationInsertDto).apply {
                this.schemaName = schemaName
            }
            locationCache.upsertLocation(locationDto)
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
        val locationDto = locationCache.getAllLocations().find { Objects.equals(it.id, locationId) }
            ?: throw UpdatingNonExistingRecordException()
        locationMapper.partialUpdate(locationUpdateDto, locationDto)
        return locationMapper.toResponseDto(locationDto)
    }
}
