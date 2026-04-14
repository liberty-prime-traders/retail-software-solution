package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigResponseDto
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigUpdateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/accounting-config")
class OrgAccountingConfigEndpoint(
    private val service: OrgAccountingConfigService
) {

    @GetMapping
    fun get(): OrgAccountingConfigResponseDto = service.get()

    @PutMapping
    fun update(@RequestBody dto: OrgAccountingConfigUpdateDto): OrgAccountingConfigResponseDto = service.update(dto)
}
