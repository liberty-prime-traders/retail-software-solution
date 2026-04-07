package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.api.DbMigrationInsertDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.LocationMigrationResponse
import me.ezra_home.retail_software_solution.platform.business.db_migration.api.OrganizationMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionService
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [DbVersionService::class, DbMigrationQualifier::class]
)
interface DbMigrationMapper {
    fun toDomainDto(entity: DbMigrationEntity): DbMigrationDto

    fun toEntity(dto: DbMigrationDto): DbMigrationEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: DbMigrationInsertDto): DbMigrationEntity

    @Mapping(target = "locations", ignore = true)
    @Mapping(source = ".", target = "organizationName", qualifiedBy = [SchemaOwnerName::class])
    @Mapping(source = "dbVersionId", target = "versionNumber", qualifiedBy = [DbVersionNumber::class])
    fun toOrganizationResponseDto(dto: DbMigrationDto): OrganizationMigrationResponseDto

    @Mapping(target = "locationId", source = "schemaOwnerId")
    @Mapping(source = ".", target = "locationName", qualifiedBy = [SchemaOwnerName::class])
    @Mapping(source = "dbVersionId", target = "versionNumber", qualifiedBy = [DbVersionNumber::class])
    fun toLocationResponseDto(dto: DbMigrationDto): LocationMigrationResponse
}
