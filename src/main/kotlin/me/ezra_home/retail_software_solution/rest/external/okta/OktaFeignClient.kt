package me.ezra_home.retail_software_solution.rest.external.okta

import me.ezra_home.retail_software_solution.business.sysuser.okta.OktaUserDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "OktaFeign", url = "UNUSED", configuration = [OktaFeignConfiguration::class])
interface OktaFeignClient {

    @GetMapping("/users")
    fun getAllUsers(): Collection<OktaUserDto>
}
