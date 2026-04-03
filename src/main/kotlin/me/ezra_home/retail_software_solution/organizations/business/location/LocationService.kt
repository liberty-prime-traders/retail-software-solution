package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
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
        return locationCache.getAllLocations().map {
            locationMapper.toResponseDto(it)
        }
    }

    fun createLocation(locationInsertDto: LocationInsertDto): LocationResponseDto {
        locationValidator.validateLocationInsert(locationInsertDto)
        val schemaName = createLocationSchema(locationInsertDto.name!!)
        try {
            val locationEntity = locationMapper.toEntity(locationInsertDto).apply {
                this.schemaName = schemaName
            }
            locationCache.upsertLocation(locationEntity)
            return locationMapper.toResponseDto(locationEntity)
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

    fun reinsertLocation(entity: LocationEntity): LocationEntity {
        entity.id = null
        locationCache.upsertLocation(entity)
        return entity
    }

    fun updateLocation(locationUpdateDto: LocationUpdateDto): LocationResponseDto {
        val locationId = SessionContextProvider.getLocationId()
        locationValidator.validateLocationUpdate(locationUpdateDto)
        val locationEntity = locationCache.getAllLocations().find { Objects.equals(it.id, locationId) }
        if (locationEntity == null) {
            throw UpdatingNonExistingRecordException()
        }
        locationMapper.partialUpdate(locationUpdateDto, locationEntity)
        return locationMapper.toResponseDto(locationEntity)
    }
}
