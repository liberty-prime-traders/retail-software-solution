package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementCapitalRequest
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementDrawRequest
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementResponse
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementSearchService
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementService
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementTransferRequest
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementVoidRequest
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/fund-movements")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class FundMovementEndpoint(
    private val fundMovementService: FundMovementService,
    private val fundMovementSearchService: FundMovementSearchService
) {

    @PostMapping("transfer")
    fun createTransfer(@RequestBody fundMovementTransferRequest: FundMovementTransferRequest): FundMovementResponse =
        fundMovementService.createTransfer(fundMovementTransferRequest)

    @PostMapping("capital")
    fun createCapital(@RequestBody fundMovementCapitalRequest: FundMovementCapitalRequest): FundMovementResponse =
        fundMovementService.createCapital(fundMovementCapitalRequest)

    @PostMapping("draw")
    fun createDraw(@RequestBody fundMovementDrawRequest: FundMovementDrawRequest): FundMovementResponse =
        fundMovementService.createDraw(fundMovementDrawRequest)

    @PostMapping("void")
    fun voidFundMovement(@RequestBody fundMovementVoidRequest: FundMovementVoidRequest): FundMovementResponse =
        fundMovementService.voidFundMovement(fundMovementVoidRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<FundMovementSearchParameters, String>): PageResponse<FundMovementResponse, String> =
        fundMovementSearchService.search(pageRequest)
}
