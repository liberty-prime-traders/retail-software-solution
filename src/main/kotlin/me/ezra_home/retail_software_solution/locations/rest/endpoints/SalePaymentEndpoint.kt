package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchParameters
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchResultDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchService
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSummaryResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentVoidCreateDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/sale-payments")
class SalePaymentEndpoint(
    private val salePaymentService: SalePaymentService,
    private val salePaymentSearchService: SalePaymentSearchService,
) {

    @PostMapping("void")
    fun voidPayment(@RequestBody dto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        return salePaymentService.voidPayment(dto)
    }

    @PostMapping("search")
    fun search(
        @RequestBody pageRequest: PageRequest<SalePaymentSearchParameters, String>
    ): PageResponse<SalePaymentSearchResultDto, String> {
        return salePaymentSearchService.search(pageRequest)
    }

    @PostMapping("summary")
    fun searchSummary(@RequestBody salePaymentSearchParameters: SalePaymentSearchParameters): SalePaymentSummaryResponseDto {
        return salePaymentSearchService.summarize(salePaymentSearchParameters)
    }
}
