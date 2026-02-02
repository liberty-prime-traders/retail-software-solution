package me.ezra_home.retail_software_solution.platform.business.sysuser.mapping

import com.okta.sdk.resource.user.User
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserDto
import me.ezra_home.retail_software_solution.platform.model.SysUserEntity
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.util.UUID
import java.util.function.Supplier

@Mapper(config = RtsMapperConfig::class)
interface SysUserMapper {
    @Mapping(source = "id", target = "oktaId")
    @Mapping(target = "id", expression = "java(idSupplier.get())")
    @Mapping(source = "profile.firstName", target = "firstName")
    @Mapping(source = "profile.lastName", target = "lastName")
    @Mapping(source = "profile.mobilePhone", target = "mobilePhone")
    @Mapping(source = "profile.secondEmail", target = "secondEmail")
    @Mapping(source = "profile.email", target = "email")
    @Mapping(target = "userType", constant = "END_USER")
    fun oktaToSystemUser(oktaUserDto: User?, @Context idSupplier: Supplier<UUID?>): SysUserDto

    @Mapping(source = "localFirstName", target = "firstName")
    @Mapping(source = "localLastName", target = "lastName")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "mobilePhone", ignore = true)
    @Mapping(target = "secondEmail", ignore = true)
    @Mapping(target = "email", ignore = true)
    fun sysUserEntityToSysUserDto(sysUserEntity: SysUserEntity): SysUserDto
}
