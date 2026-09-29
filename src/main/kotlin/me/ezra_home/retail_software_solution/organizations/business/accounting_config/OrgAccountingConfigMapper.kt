package me.ezra_home.retail_software_solution.organizations.business.accounting_config

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigInsertDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrgAccountingConfigMapper {

    fun toDomainDto(entity: OrgAccountingConfigEntity): OrgAccountingConfigDto

    fun toEntity(dto: OrgAccountingConfigDto): OrgAccountingConfigEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(insertDto: OrgAccountingConfigInsertDto): OrgAccountingConfigEntity

    fun toResponseDto(dto: OrgAccountingConfigDto): OrgAccountingConfigResponseDto
}
