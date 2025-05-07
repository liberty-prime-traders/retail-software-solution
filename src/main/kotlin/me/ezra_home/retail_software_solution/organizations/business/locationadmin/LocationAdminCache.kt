package me.ezra_home.retail_software_solution.organizations.business.locationadmin

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.model.LocationAdminEntity
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import org.springframework.cache.annotation.CacheConfig
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.LOCATION_ADMIN])
class LocationAdminCache(private val locationAdminRepository: LocationAdminRepository) {

    fun getAdminHistoryForLocation(locationId: UUID?): Collection<LocationAdminEntity> {
        return locationId?.let { locationAdminRepository.findByLocationId(it) }
            ?: throw QueriedByEmptyIdException()
    }

    fun upsertLocationAdmin(locationAdminEntity: LocationAdminEntity) {
        locationAdminRepository.save(locationAdminEntity)
    }
}
