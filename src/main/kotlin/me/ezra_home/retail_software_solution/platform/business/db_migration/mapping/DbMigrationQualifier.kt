package me.ezra_home.retail_software_solution.platform.business.db_migration.mapping

import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SchemaOwnerName


@Component
class DbMigrationQualifier(
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache
) {
    @SchemaOwnerName
    fun getSchemaOwnerName(entity: DbMigrationEntity): String? {
        return when (entity.schemaOwnerType) {
            SchemaOwnerType.ORGANIZATION ->
                organizationCache.getAllOrganizations().find { it.id == entity.schemaOwnerId }?.name

            SchemaOwnerType.LOCATION ->
                locationCache.getAllLocations().find { it.id == entity.schemaOwnerId }?.name

        }
    }
}
