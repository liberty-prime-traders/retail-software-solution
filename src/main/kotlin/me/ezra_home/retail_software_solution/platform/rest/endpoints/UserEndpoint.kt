package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.platform.business.sysuser.`public`.SysUserWithProfileDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.`public`.SysUserService
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
    fun createUser(): SysUserWithProfileDto = userService.addSystemUser()

    @GetMapping
    fun getAllUsers(): Collection<SysUserWithProfileDto> = userService.getAllUsers()
}
