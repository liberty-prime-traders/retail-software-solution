package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface FiscalPeriodMapper {

    fun toDomainDto(entity: FiscalPeriodEntity): FiscalPeriodDto

    fun toEntity(dto: FiscalPeriodDto): FiscalPeriodEntity
}
