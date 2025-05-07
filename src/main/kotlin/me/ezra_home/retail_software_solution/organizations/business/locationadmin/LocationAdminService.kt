package me.ezra_home.retail_software_solution.organizations.business.locationadmin

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
import me.ezra_home.retail_software_solution.organizations.model.LocationAdminEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID


@Service
@TransactionalOnOrganizationSchema
class LocationAdminService(
    private val locationAdminMapper: LocationAdminMapper,
    private val locationAdminCache: LocationAdminCache,
    private val sysUserService: SysUserService,
    private val locationCache: LocationCache
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAdminHistoryForLocation(): Collection<LocationAdminResponseDto> {
        val locationId = SessionContextProvider.getLocationId()
        return locationAdminCache.getAdminHistoryForLocation(locationId)
            .map { locationAdminMapper.toResponseDto(it) }
    }

    fun createLocationAdmin(adminId: UUID): LocationAdminResponseDto {
        val locationId = SessionContextProvider.getLocationId()
        validateUserAndLocationExist(adminId, locationId)
        val entity = LocationAdminEntity(locationId).apply { this.adminId = adminId }
        locationAdminCache.upsertLocationAdmin(entity)
        return locationAdminMapper.toResponseDto(entity)
    }

    private fun validateUserAndLocationExist(adminId: UUID, locationId: UUID?) {
        locationCache.getAllLocations().find { it.id == locationId }
            ?: throw RtsGenericException("Location with id $locationId not found")
        sysUserService.getAllUsers().find { it.id == adminId }
            ?: throw RtsGenericException("User with id $adminId not found")
    }

    fun terminateLocationAdmin(adminId: UUID) {
        val locationId = SessionContextProvider.getLocationId()
        locationAdminCache.getAdminHistoryForLocation(locationId)
            .find { it.isActive() && it.adminId == adminId }
            ?.let {
                it.endOn = OffsetDateTime.now()
                locationAdminCache.upsertLocationAdmin(it)
            }
    }
}
