package me.ezra_home.retail_software_solution.platform.business.location

import me.ezra_home.retail_software_solution.platform.business.location.LocationService.Companion.NAME_ALREADY_EXISTS
import me.ezra_home.retail_software_solution.platform.business.location.LocationService.Companion.NAME_IS_REQUIRED
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.location.dto.LocationUpdateDto
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Component
class LocationValidator(private val locationCache: LocationCache) {

    fun validateLocationInsert(locationInsertDto: LocationInsertDto, organizationId: UUID) {
        val name = Optional.ofNullable(locationInsertDto.name)
        if (name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        locationCache.getByOrganizationId(organizationId)
            .find { it.name.equals(locationInsertDto.name, ignoreCase = true) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.get())) }
    }

    fun validateLocationUpdate(locationUpdateDto: LocationUpdateDto, organizationId: UUID) {
        val name = locationUpdateDto.name
        if (name == null || name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        val locationId = SessionContextProvider.getLocationId()
        locationCache.getByOrganizationId(organizationId)
            .find { it.name.equals(name.get(), ignoreCase=true) && !Objects.equals(it.id, locationId) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.get())) }
    }
}
