package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.organizations.business.location.LocationService.Companion.NAME_ALREADY_EXISTS
import me.ezra_home.retail_software_solution.organizations.business.location.LocationService.Companion.NAME_IS_REQUIRED
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects

@Component
class LocationValidator(private val locationCache: LocationCache) {

    fun validateLocationInsert(locationInsertDto: LocationInsertDto) {
        val name = StringUtils.getValueOrException(locationInsertDto.name, NAME_IS_REQUIRED)
        locationCache.getAllLocations()
            .find { StringUtils.isEquivalent(it.name, locationInsertDto.name) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun validateLocationUpdate(locationUpdateDto: LocationUpdateDto) {
        val name = StringUtils.getValueOrException(locationUpdateDto.name, NAME_IS_REQUIRED)
        val locationId = SessionContextProvider.getLocationId()
        locationCache.getAllLocations()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, locationId) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }
}
