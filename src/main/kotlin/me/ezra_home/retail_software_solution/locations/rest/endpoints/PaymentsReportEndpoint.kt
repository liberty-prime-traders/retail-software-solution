package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentRowDto
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchParameters
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchService
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSummaryResponseDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/payments")
class PaymentsReportEndpoint(
    private val paymentSearchService: PaymentSearchService
) {

    @PostMapping("search")
    fun search(
        @RequestBody pageRequest: PageRequest<PaymentSearchParameters, String>
    ): PageResponse<PaymentRowDto, String> {
        return paymentSearchService.search(pageRequest)
    }

    @PostMapping("summary")
    fun searchSummary(@RequestBody paymentSearchParameters: PaymentSearchParameters): PaymentSummaryResponseDto {
        return paymentSearchService.summarize(paymentSearchParameters)
    }
}
