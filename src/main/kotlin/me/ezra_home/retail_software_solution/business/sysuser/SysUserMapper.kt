package me.ezra_home.retail_software_solution.business.sysuser

import me.ezra_home.retail_software_solution.business.sysuser.okta.OktaUserDto
import me.ezra_home.retail_software_solution.business.util.mapper.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface SysUserMapper {
    @Mapping(source = "id", target = "oktaId")
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "profile.firstName", target = "firstName")
    @Mapping(source = "profile.lastName", target = "lastName")
    @Mapping(source = "profile.mobilePhone", target = "mobilePhone")
    @Mapping(source = "profile.secondEmail", target = "secondEmail")
    @Mapping(source = "profile.login", target = "login")
    @Mapping(source = "profile.email", target = "email")
    fun oktaToSystemUser(oktaUserDto: OktaUserDto?): SysUserDto
}
