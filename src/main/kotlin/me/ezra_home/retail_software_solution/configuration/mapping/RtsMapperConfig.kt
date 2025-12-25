package me.ezra_home.retail_software_solution.configuration.mapping

import me.ezra_home.retail_software_solution.platform.business.db_migration.mapping.DbMigrationQualifier
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.UserQualifier
import me.ezra_home.retail_software_solution.util.business.mappers.OptionalQualifier
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
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
        DbMigrationQualifier::class
    ]
)
interface RtsMapperConfig
