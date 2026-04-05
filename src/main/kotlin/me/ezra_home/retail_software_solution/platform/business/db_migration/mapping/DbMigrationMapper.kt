package me.ezra_home.retail_software_solution.platform.business.db_migration.mapping

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.LocationMigrationResponse
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.OrganizationMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [DbVersionService::class, DbMigrationQualifier::class]
)
interface DbMigrationMapper {
    fun toDomainDto(entity: DbMigrationEntity): DbMigrationDto

    fun toEntity(dto: DbMigrationDto): DbMigrationEntity

    @Mapping(target = "locations", ignore = true)
    @Mapping(source = ".", target = "organizationName", qualifiedBy = [SchemaOwnerName::class])
    @Mapping(source = "dbVersionId", target = "versionNumber", qualifiedBy = [DbVersionNumber::class])
    fun toOrganizationResponseDto(dto: DbMigrationDto): OrganizationMigrationResponseDto

    @Mapping(target = "locationId", source = "schemaOwnerId")
    @Mapping(source = ".", target = "locationName", qualifiedBy = [SchemaOwnerName::class])
    @Mapping(source = "dbVersionId", target = "versionNumber", qualifiedBy = [DbVersionNumber::class])
    fun toLocationResponseDto(dto: DbMigrationDto): LocationMigrationResponse
}
