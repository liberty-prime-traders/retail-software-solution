package me.ezra_home.retail_software_solution.platform.business.subdomain

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ReservedSubdomainMaper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(entity: ReservedSubdomainEntity): ReservedSubdomainDto
}
