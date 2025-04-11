package me.ezra_home.retail_software_solution.platform.business.location

import me.ezra_home.retail_software_solution.platform.business.location.LocationService.Companion.NAME_ALREADY_EXISTS
import me.ezra_home.retail_software_solution.platform.business.location.LocationService.Companion.NAME_IS_REQUIRED
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.UUID

@Component
class LocationValidator(private val locationCache: LocationCache) {

    fun validateLocationInsert(locationInsertDto: LocationInsertDto, organizationId: UUID) {
        val name = StringUtils.getValueOrException(locationInsertDto.name, NAME_IS_REQUIRED)
        locationCache.getByOrganizationId(organizationId)
            .find { StringUtils.isEquivalent(it.name, locationInsertDto.name) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun validateLocationUpdate(locationUpdateDto: LocationUpdateDto, organizationId: UUID) {
        val name = StringUtils.getValueOrException(locationUpdateDto.name, NAME_IS_REQUIRED)
        val locationId = SessionContextProvider.getLocationId()
        locationCache.getByOrganizationId(organizationId)
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, locationId) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }
}
