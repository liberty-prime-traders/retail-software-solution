package me.ezra_home.retail_software_solution.rest.endpoints.test

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("test")
class UnsecuredTestController {

    @GetMapping("unrestricted")
    fun unrestricted(): String {
        return "You have reached a leaked base location"
    }
}
