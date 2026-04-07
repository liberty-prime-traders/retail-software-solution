package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.api.ReservedSubdomainDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ReservedSubdomainMaper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(entity: ReservedSubdomainEntity): ReservedSubdomainDto
}
