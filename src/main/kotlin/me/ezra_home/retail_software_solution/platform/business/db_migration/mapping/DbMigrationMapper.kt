package me.ezra_home.retail_software_solution.platform.business.db_migration.mapping

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.LocationMigrationResponse
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.OrganizationMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionQualifier
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [DbVersionQualifier::class, DbMigrationQualifier::class]
)
internal interface DbMigrationMapper {
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
