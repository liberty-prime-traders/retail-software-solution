package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentService
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentVoidRequest
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanSearchService
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanService
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanVoidRequest
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/loans")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class LoanEndpoint(
    private val loanService: LoanService,
    private val loanRepaymentService: LoanRepaymentService,
    private val loanSearchService: LoanSearchService
) {

    @PostMapping
    fun createLoan(@RequestBody loanCreateRequest: LoanCreateRequest): LoanResponse =
        loanService.createLoan(loanCreateRequest)

    @PostMapping("repayments")
    fun recordRepayment(@RequestBody loanRepaymentCreateRequest: LoanRepaymentCreateRequest): LoanResponse =
        loanRepaymentService.recordRepayment(loanRepaymentCreateRequest)

    @PostMapping("void")
    fun voidLoan(@RequestBody loanVoidRequest: LoanVoidRequest): LoanResponse =
        loanService.voidLoan(loanVoidRequest)

    @PostMapping("repayments/void")
    fun voidRepayment(@RequestBody loanRepaymentVoidRequest: LoanRepaymentVoidRequest): LoanResponse =
        loanRepaymentService.voidRepayment(loanRepaymentVoidRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<LoanSearchParameters, String>): PageResponse<LoanResponse, String> =
        loanSearchService.search(pageRequest)

    @GetMapping("{loanReference}")
    fun getLoan(@PathVariable loanReference: String): LoanResponse =
        loanService.getLoan(loanReference)
}
