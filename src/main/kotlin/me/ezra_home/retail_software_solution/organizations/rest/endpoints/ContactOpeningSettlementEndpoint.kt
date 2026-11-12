package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementService
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementSummaryResponse
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementVoidRequest
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/contact-opening-settlements")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ContactOpeningSettlementEndpoint(
    private val contactOpeningSettlementService: ContactOpeningSettlementService
) {

    @PostMapping
    fun recordSettlement(
        @RequestBody contactOpeningSettlementCreateRequest: ContactOpeningSettlementCreateRequest
    ): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementService.recordSettlement(contactOpeningSettlementCreateRequest)

    @PostMapping("void")
    fun voidSettlement(
        @RequestBody contactOpeningSettlementVoidRequest: ContactOpeningSettlementVoidRequest
    ): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementService.voidSettlement(contactOpeningSettlementVoidRequest)

    @GetMapping
    fun getSummary(@RequestParam contactReferenceNumber: String): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementService.getSummary(contactReferenceNumber)
}
