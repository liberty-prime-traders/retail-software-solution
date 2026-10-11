package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceRevisionDto
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceService
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceSetDto
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/contact-opening-balances")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ContactOpeningBalanceEndpoint(
    private val contactOpeningBalanceService: ContactOpeningBalanceService
) {

    @PostMapping("{contactId}")
    fun set(
        @PathVariable contactId: UUID,
        @RequestBody contactOpeningBalanceSetDto: ContactOpeningBalanceSetDto
    ) = contactOpeningBalanceService.set(contactId, contactOpeningBalanceSetDto)

    @GetMapping("history")
    fun getHistory(@RequestParam contactReferenceNumber: String): List<ContactOpeningBalanceRevisionDto> =
        contactOpeningBalanceService.getHistory(contactReferenceNumber)
}
