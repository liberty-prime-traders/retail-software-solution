package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
internal interface ReservedSubdomainMaper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(entity: ReservedSubdomainEntity): ReservedSubdomainDto
}
