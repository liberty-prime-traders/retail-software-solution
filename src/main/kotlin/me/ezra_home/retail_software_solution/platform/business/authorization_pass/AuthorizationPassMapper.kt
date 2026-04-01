package me.ezra_home.retail_software_solution.platform.business.authorization_pass

import me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto.AuthorizationPassResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.platform.model.AuthorizationPassEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface AuthorizationPassMapper {

    @Mapping(source = "assignedToId", target = "assignedTo", qualifiedBy = [FullName::class])
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(entity: AuthorizationPassEntity): AuthorizationPassResponseDto
}
