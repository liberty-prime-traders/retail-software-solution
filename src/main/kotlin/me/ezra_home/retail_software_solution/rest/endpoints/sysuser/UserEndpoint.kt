package me.ezra_home.retail_software_solution.rest.endpoints.sysuser

import me.ezra_home.retail_software_solution.business.sysuser.SysUserDto
import me.ezra_home.retail_software_solution.business.sysuser.SysUserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/secured/user")
class UserEndpoint(private val userService: SysUserService) {

    @PostMapping
    fun createUser(): SysUserDto = userService.addSystemUser()
}
