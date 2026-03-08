package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto.AuthorizationPassQualifier
import me.ezra_home.retail_software_solution.platform.business.db_migration.mapping.DbMigrationQualifier
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionQualifier
import org.mapstruct.InjectionStrategy
import org.mapstruct.MapperConfig
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = [
        UserQualifier::class,
        OptionalQualifier::class,
        StringQualifier::class,
        DbMigrationQualifier::class,
        DbVersionQualifier::class,
        AuthorizationPassQualifier::class,
        DateQualifier::class,
        EnumQualifier::class
    ]
)
interface RtsMapperConfig
