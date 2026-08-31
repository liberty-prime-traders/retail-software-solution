package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.opening_balance.api.OpeningBalanceRevisionDto
import me.ezra_home.retail_software_solution.organizations.business.opening_balance.api.OpeningBalanceService
import me.ezra_home.retail_software_solution.organizations.business.opening_balance.api.OpeningBalanceUpsertDto
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/opening-balances")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class OpeningBalanceController(
    private val openingBalanceService: OpeningBalanceService
) {

    @PostMapping
    fun upsert(@RequestBody openingBalanceUpsertDto: OpeningBalanceUpsertDto) =
        openingBalanceService.upsert(openingBalanceUpsertDto)

    @GetMapping("history")
    fun getHistory(@RequestParam accountCode: String): List<OpeningBalanceRevisionDto> =
        openingBalanceService.getHistory(accountCode)
}
