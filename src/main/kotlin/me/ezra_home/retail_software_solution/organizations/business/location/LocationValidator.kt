package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects

@Component
internal class LocationValidator(private val locationCache: LocationCache) {


    companion object {
        const val NAME_IS_REQUIRED = "A location must have a name"
        const val NAME_ALREADY_EXISTS = "A location with the name %s already exists"
    }

    fun validateLocationInsert(locationInsertDto: LocationInsertDto) {
        val name = StringUtils.getValueOrException(locationInsertDto.name, NAME_IS_REQUIRED)
        locationCache.getAllLocations()
            .find { StringUtils.isEquivalent(it.name, locationInsertDto.name) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
        DateTimes.validateTimezone(locationInsertDto.timezone)
    }

    fun validateLocationUpdate(locationUpdateDto: LocationUpdateDto) {
        val name = StringUtils.getValueOrException(locationUpdateDto.name, NAME_IS_REQUIRED)
        val locationId = SessionContextProvider.getLocationId()
        locationCache.getAllLocations()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, locationId) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
        DateTimes.validateTimezone(locationUpdateDto.timezone)
    }

}
