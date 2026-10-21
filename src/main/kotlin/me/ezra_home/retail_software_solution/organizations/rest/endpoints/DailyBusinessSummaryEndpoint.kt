package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBalancesResponse
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBalancesService
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBusinessSummaryRequest
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyPerformanceResponse
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyPerformanceService
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/daily-business-summary")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class DailyBusinessSummaryEndpoint(
    private val dailyPerformanceService: DailyPerformanceService,
    private val dailyBalancesService: DailyBalancesService
) {

    @PostMapping("performance")
    fun getPerformance(@RequestBody dailyBusinessSummaryRequest: DailyBusinessSummaryRequest): DailyPerformanceResponse =
        dailyPerformanceService.getPerformance(dailyBusinessSummaryRequest)

    @PostMapping("balances")
    fun getBalances(@RequestBody dailyBusinessSummaryRequest: DailyBusinessSummaryRequest): DailyBalancesResponse =
        dailyBalancesService.getBalances(dailyBusinessSummaryRequest)
}
