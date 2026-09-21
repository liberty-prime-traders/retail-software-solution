package me.ezra_home.retail_software_solution.platform.business.authorization_pass

import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.AuthorizationPassResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.CreatorFullName
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.NullableFullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface AuthorizationPassMapper {

    @Mapping(source = "assignedToId", target = "assignedTo", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toResponseDto(entity: AuthorizationPassEntity): AuthorizationPassResponseDto
}
