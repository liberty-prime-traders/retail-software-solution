package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SchemaOwnerName


@Component
class DbMigrationQualifier(
    private val organizationService: OrganizationService,
    private val locationService: LocationService
) {
    @SchemaOwnerName
    fun getSchemaOwnerName(dto: DbMigrationDto): String? {
        return when (dto.schemaOwnerType) {
            SchemaOwnerType.ORGANIZATION ->
                organizationService.getAllOrganizationDtos().find { it.id == dto.schemaOwnerId }?.name

            SchemaOwnerType.LOCATION ->
                locationService.getAllLocationDtos().find { it.id == dto.schemaOwnerId }?.name
        }
    }
}
