package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationUpdateDto
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
