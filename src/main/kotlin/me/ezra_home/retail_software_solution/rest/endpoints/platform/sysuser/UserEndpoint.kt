package me.ezra_home.retail_software_solution.rest.endpoints.platform.sysuser

import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/users")
class UserEndpoint(private val userService: SysUserService) {

    @PostMapping
    fun createUser(): SysUserDto = userService.addSystemUser()

    @GetMapping
    fun getAllUsers(): Collection<SysUserDto> = userService.getAllUsers()
}
