package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateService
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateUpdateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/tax-rates")
class TaxRateEndpoint(private val taxRateService: TaxRateService) {

    @GetMapping
    fun getAll(): Collection<TaxRateResponseDto> = taxRateService.getAll()

    @PostMapping
    fun create(@RequestBody dto: TaxRateInsertDto): TaxRateResponseDto =
        taxRateService.create(dto)

    @PutMapping
    fun update(@RequestBody dto: TaxRateUpdateDto): TaxRateResponseDto =
        taxRateService.update(dto)
}
