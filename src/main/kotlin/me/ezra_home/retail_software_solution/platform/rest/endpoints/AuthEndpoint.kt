package me.ezra_home.retail_software_solution.platform.rest.endpoints

import me.ezra_home.retail_software_solution.platform.business.auth.api.AuthService
import me.ezra_home.retail_software_solution.platform.business.auth.api.LoginRequest
import me.ezra_home.retail_software_solution.platform.business.auth.api.LoginResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("auth")
class AuthEndpoint(private val authService: AuthService) {

    @PostMapping("login")
    fun login(@RequestBody loginRequest: LoginRequest): LoginResponse = authService.login(loginRequest)
}
