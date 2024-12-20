package me.ezra_home.retail_software_solution.configuration.mapping

import me.ezra_home.retail_software_solution.business.util.mappers.OptionalQualifier
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.UserQualifier
import org.mapstruct.InjectionStrategy
import org.mapstruct.MapperConfig
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = [UserQualifier::class, OptionalQualifier::class]
)
interface RtsMapperConfig
